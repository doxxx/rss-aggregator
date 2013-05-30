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
import java.util.{Date, Calendar}

class AggregatorService extends Actor with ActorLogging {
  import AggregatorService._

  implicit def executionContext = context.dispatcher

  implicit val timeout = Timeout(120.seconds)

  private val feedFetcher = new FeedFetcher(context.system)

  override def preStart() {
    log.info("Loading known feeds")
    future {
      FeedDAO.findAll.foreach(scheduleFeedUpdate(_))
    }
  }

  override def receive = LoggingReceive {
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

  def scheduleFeedUpdate(feed: Feed) {
    val next = feed.lastFetchTime.map(_.getTime).getOrElse(0L) + 15.minutes.toMillis
    val delay = (next - System.currentTimeMillis()).max(0)
    log.debug("Scheduling next check in {} milliseconds for feed: {}", delay, feed.link)
    context.system.scheduler.scheduleOnce(delay.millis) {
      checkForUpdates(feed).foreach(scheduleFeedUpdate(_))
    }
  }

  private def checkForUpdates(feed: Feed): Future[Feed] = {
    log.info("Checking for updates to feed: {}", feed.link)
    feedFetcher(feed.link).map {
      case (updatedFeed, articles) => {
        FeedDAO.save(updatedFeed)
        articles.foreach(ArticleDAO.save(_))
        updatedFeed
      }
    }.recover {
      case t: Throwable => {
        log.error(t, "Error while checking feed for updates: {}", feed.link)
        feed.copy(lastFetchTime = Some(new Date()))
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

object AggregatorService {
  case class AddFeed(url: String)
  case class AddFeedResult(feed: Feed)
  case class ImportOpml(opml: String)
}
