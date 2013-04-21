package net.doxxx.rssaggregator

import akka.actor.{ActorLogging, Actor}
import akka.pattern._
import akka.util.Timeout
import model._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import com.sun.syndication.feed.synd._
import scala.concurrent._
import com.sun.syndication.io.SyndFeedInput
import java.io.{StringReader, InputStreamReader}
import java.net.URL
import scala.util.Failure
import scala.util.Success
import scala.xml.XML

class Aggregator extends Actor with ActorLogging {
  import Aggregator._
  import context.dispatcher

  implicit val timeout = Timeout(30.seconds)

  def receive = {
    case Start => {
      log.info("Loading known feeds")
      future {
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
        case Success(syndFeed) => {
          log.debug("Fetched feed {} containing {} articles", syndFeed.getTitle, syndFeed.getEntries.size())
          // store feed and articles in db
          storeFeed(url, syndFeed)
          storeArticles(url, syndFeed)
        }
        case Failure(t) => {
          log.error(t, "Could not load feed {}", url)
        }
      }.map { syndFeed =>
        syndFeed.getTitle
      }.pipeTo(sender)
    }
    case ImportOpml(opml) => {
      val feeds = importOpml(opml)
      feeds.foreach { f =>
        log.debug("Fetching feed {}", f.link)
        fetchFeed(f.link).onComplete {
          case Success(sf) => {
            if (FeedDAO.findOneById(f.link).isEmpty) {
              log.debug("Saving new feed: {}", f.link)
              FeedDAO.save(f)
              storeArticles(f.link, sf)
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
      case Success(sf) => {
        storeFeed(feed.link, sf)
        storeArticles(feed.link, sf)
      }
      case Failure(t) => log.error(t, "Could not check feed for updates: {}", feed.link)
    }
  }

  private def fetchFeed(url: String): Future[SyndFeed] = future {
    val c = new URL(url).openConnection()
    c.setConnectTimeout(20000)
    new SyndFeedInput().build(new InputStreamReader(c.getInputStream))
  }

  private def storeFeed(url: String, syndFeed: SyndFeed) {
    future {
      if (FeedDAO.findOneById(url).isEmpty) {
        val feed = Feed(url, syndFeed.getLink, syndFeed.getTitle, Option(syndFeed.getDescription))
        log.debug("Storing feed {}", feed.title)
        FeedDAO.save(feed)
      }
    }
  }

  private def storeArticles(feedLink: String, syndFeed: SyndFeed) {
    syndFeed.getEntries.map(_.asInstanceOf[SyndEntry]).foreach { e =>
      val id = Article.makeId(feedLink, e)
      try {
        ArticleDAO.findOneById(id) match {
          case Some(article) => {
            // TODO: Check if it's updated?
            log.debug("Skipping already stored article {}", id)
          }
          case None => {
            val article = Article.make(id, feedLink, e)
            log.debug("Storing article {}", article._id)
            ArticleDAO.save(article)
          }
        }
      }
      catch {
        case ex:Exception => log.error(ex, "Couldn't store article: {}", e.toString)
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
  case class ImportOpml(opml: String)
}
