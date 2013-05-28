package net.doxxx.rssaggregator

import model._
import akka.actor.{ActorLogging, Actor}
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent._
import scala.util.{Failure, Success}
import scala.xml.XML
import java.io.StringReader
import akka.event.LoggingReceive

class AggregatorService extends Actor with ActorLogging {
  import AggregatorService._

  implicit def executionContext = context.dispatcher

  implicit val timeout = Timeout(120.seconds)

  private val feedFetcher = new FeedFetcher(context.system)

  def receive = LoggingReceive {
    case Start => {
      log.info("Loading known feeds")
      future {
        FeedDAO.findAll.foreach(checkForUpdates)
      }
      context.system.scheduler.schedule(1.hour, 1.hour) {
        FeedDAO.findAll.foreach(checkForUpdates)
      }
    }

    case AddFeed(url) => {
      log.debug("Fetching feed {}", url)

      feedFetcher(url).andThen {
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
        feedFetcher(f.link).onComplete {
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
    feedFetcher(feed.link).onComplete {
      case Success((updatedFeed, articles)) => {
        FeedDAO.save(updatedFeed)
        articles.foreach(ArticleDAO.save(_))
      }
      case Failure(t) => log.error(t, "Could not check feed for updates: {}", feed.link)
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
  case class AddFeed(url: String)
  case class AddFeedResult(feed: Feed)
  case class ImportOpml(opml: String)
}
