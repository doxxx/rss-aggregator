package net.doxxx.rssaggregator

import model._
import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.pattern._
import akka.util.Timeout
import akka.io.IO
import spray.can.Http
import spray.client.pipelining._
import com.sun.syndication.io.SyndFeedInput
import scala.concurrent.duration._
import scala.concurrent._
import scala.util.{Failure, Success}
import scala.xml.XML
import java.io.StringReader
import spray.http.HttpEntity
import com.sun.syndication.feed.synd.SyndFeed

class AggregatorService extends Actor with ActorLogging {
  import AggregatorService._

  implicit def executionContext = context.dispatcher

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

  implicit def entity2SyndFeed(entity: HttpEntity): SyndFeed = new SyndFeedInput().build(new StringReader(entity.asString))

  val httpClient = IO(Http)(context.system)
  val pipeline = sendReceive(httpClient) ~> unmarshal[SyndFeed]

  private def fetchFeed(url: String): Future[(Feed, Seq[Article])] = {
    pipeline(Get(url)) map { sf =>
      log.debug("Parsed feed {}", sf.getTitle)
      val (feed, articles) = DAO.fromSyndFeed(url, sf)
      log.debug("Fetched feed {} containing {} articles", feed.title, articles.length)
      (feed, articles)
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

object AggregatorService {
  case object Start
  case object GetAllFeeds
  case class GetFeedArticles(feedLink: String)
  case class AddFeed(url: String)
  case class AddFeedResult(feed: Feed)
  case class ImportOpml(opml: String)
}
