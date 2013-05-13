package net.doxxx.rssaggregator

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.pattern._
import akka.util.Timeout
import spray.client.HttpConduit
import spray.io._
import model._
import scala.concurrent.duration._
import scala.concurrent._
import com.sun.syndication.io.SyndFeedInput
import java.io.StringReader
import java.net.URL
import scala.util.{Try, Failure, Success}
import scala.xml.XML
import spray.can.client.HttpClient
import spray.caching.{Cache, LruCache}
import com.mongodb.casbah.commons.MongoDBObject

class Aggregator extends Actor with ActorLogging {
  import Aggregator._
  import context.dispatcher

  private val ioBridge = IOExtension(context.system).ioBridge()
  private val httpClient = context.system.actorOf(Props(new HttpClient(ioBridge)))

  implicit val timeout = Timeout(120.seconds)

  def receive = {
    case Start => {
      log.info("Loading known feeds")
      future {
        FeedDAO.findAll.foreach(checkForUpdates)
      }
      context.system.scheduler.schedule(1.hour, 1.hour) {
        FeedDAO.findAll.foreach(checkForUpdates)
      }
    }
    case GetAllFeeds => {
      future { FeedDAO.findAll.toSeq } pipeTo sender
    }
    case GetFeedArticles(feedLink) => {
      future { ArticleDAO.findByFeedLink(feedLink).toSeq } pipeTo sender
    }
    case AddFeed(url) => {
      log.debug("Fetching feed {}", url)

      fetchFeed(url).andThen {
        case Success((feed, articles)) => {
          FeedDAO.save(feed)
          articles.foreach(ArticleDAO.save(_))
        }
        case Failure(t) => {
          log.error(t, "Could not load feed {}", url)
        }
      }.map {
        case (feed, articles) => AddFeedResult(feed)
      }.pipeTo(sender)
    }
    case ImportOpml(opml) => {
      val feeds = importOpml(opml)
      feeds.foreach { f =>
        log.debug("Fetching feed {}", f.link)
        fetchFeed(f.link).onComplete {
          case Success((updatedFeed, articles)) => {
            if (FeedDAO.findOneById(f.link).isEmpty) {
              log.debug("Saving new feed: {}", f.link)
              FeedDAO.save(updatedFeed)
              articles.foreach(ArticleDAO.save(_))
            }
            else {
              log.debug("Skipping known feed: {}", f.link)
            }
          }
          case Failure(t) => {
            log.error(t, "Could not import feed {}", f.link)
          }
        }
      }
    }
    case Authenticate(email, password) => {
      future {
        UserDAO.findOne(MongoDBObject("_id" -> email, "password" -> password))
      } pipeTo sender
    }
  }

  private def checkForUpdates(feed: Feed) {
    fetchFeed(feed.link).onComplete {
      case Success((updatedFeed, articles)) => {
        FeedDAO.save(updatedFeed)
        articles.foreach(ArticleDAO.save(_))
      }
      case Failure(t) => log.error(t, "Could not check feed for updates: {}", feed.link)
    }
  }

  val httpConduits: Cache[ActorRef] = LruCache()

  private def fetchFeed(url: String): Future[(Feed, Seq[Article])] = {
    import HttpConduit._
    val u = new URL(url)
    val host = u.getHost
    val port = if (u.getPort == -1) u.getDefaultPort else u.getPort
    httpConduits.fromFuture(u.getAuthority) {
      future {
        context.system.actorOf(
          props = Props(new HttpConduit(httpClient, host, port, sslEnabled = u.getProtocol == "https")),
          name = "http-conduit-" + host.replace('.', '_')
        )
      }
    }.map { conduit =>
      sendReceive(conduit)
    }.flatMap { pipeline =>
      val response = pipeline(Get(u.getFile))
      response.map { r =>
        val sf = new SyndFeedInput().build(new StringReader(r.entity.asString))
        log.debug("Parsed feed {}", sf.getTitle)
        val (feed, articles) = DAO.fromSyndFeed(url, sf)
        log.debug("Fetched feed {} containing {} articles", feed.title, articles.length)
        (feed, articles)
      }
    }
  }

  private def importOpml(opml: String): Seq[Feed] = {
    val root = XML.load(new StringReader(opml))
    val folders = root \ "body" \ "outline"
    folders.flatMap { elem =>
      val folderName = (elem \ "@title").text
      val feeds = elem \ "outline"
      feeds map { elem =>
        val feedLink = (elem \ "@xmlUrl").text
        val siteLink = (elem \ "@htmlUrl").text
        val title = (elem \ "@title").text
        Feed(feedLink, siteLink, title, None, Set(folderName))
      }
    }
  }
}

object Aggregator {
  case object Start
  case object GetAllFeeds
  case class GetFeedArticles(feedLink: String)
  case class AddFeed(url: String)
  case class AddFeedResult(feed: Feed)
  case class ImportOpml(opml: String)

  case class Authenticate(email: String, password: String)
}
