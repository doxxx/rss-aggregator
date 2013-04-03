package net.doxxx.rssaggregator

import akka.actor.{ActorLogging, Props, Actor}
import akka.pattern._
import akka.util.Timeout
import model._
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import com.sun.syndication.feed.synd._
import scala.util.{Failure, Success}
import scala.concurrent._
import com.sun.syndication.io.SyndFeedInput
import java.io.InputStreamReader
import java.net.URL

class Aggregator extends Actor with ActorLogging {
  import Aggregator._
  import context.dispatcher

  val feedStorage = context.actorOf(Props[FeedStorage], "feed-storage")
  val articleStorage = context.actorOf(Props[ArticleStorage], "article-storage")
  val opmlImporter = context.actorOf(Props[OpmlImporter], "opml-importer")

  implicit val timeout = Timeout(30.seconds)

  def receive = {
    case Start => {
      log.info("Loading known feeds")
      getAllFeeds.onSuccess {
        case feeds: Seq[Feed] => feeds.foreach { f =>
          self ! AddFeed(f.link)
        }
      }
    }
    case GetAllFeeds => {
      getAllFeeds pipeTo sender
    }
    case GetFeedArticles(feedLink) => {
      getFeedArticles(feedLink) pipeTo sender
    }
    case AddFeed(url) => {
      log.debug("Fetching feed {}", url)

      fetchFeed(url).onComplete {
        case Success(syndFeed) => {
          log.debug("Fetched feed {} containing {} articles", syndFeed.getTitle, syndFeed.getEntries.size())
          storeFeed(url, syndFeed)
          storeArticles(url, syndFeed)
        }
        case Failure(t) => {
          log.error(t, "Could not load feed {}", url)
        }
      }

      // schedule future check
      context.system.scheduler.scheduleOnce(1.hour, self, AddFeed(url))
    }
    case ImportOpml(opml) => {
      importOpml(opml).onComplete {
        case Success(feeds) => {
          feeds.foreach { feed =>
            self ! AddFeed(feed.link)
          }
        }
        case Failure(t) => {
          log.error(t, "Could not import OPML")
        }
      }
    }
  }


  private def importOpml(opml: String): Future[Seq[Feed]] = {
    (opmlImporter ? OpmlImporter.Import(opml)).mapTo[Seq[Feed]]
  }

  private def getFeedArticles(feedLink: String): Future[Seq[Article]] = {
    (articleStorage ? ArticleStorage.GetFeedArticles(feedLink)).mapTo[Seq[Article]]
  }

  private def getAllFeeds: Future[Seq[Feed]] = {
    (feedStorage ? FeedStorage.GetAllFeeds).mapTo[Seq[Feed]]
  }

  private def fetchFeed(url: String): Future[SyndFeed] = future {
    new SyndFeedInput().build(new InputStreamReader(new URL(url).openStream()))
  }

  private def storeFeed(url: String, syndFeed: SyndFeed) {
    feedStorage ! FeedStorage.StoreFeed(Feed(url, syndFeed.getLink, syndFeed.getTitle, Option(syndFeed.getDescription)))
  }

  private def storeArticles(url: String, syndFeed: SyndFeed) {
    syndFeed.getEntries.map(_.asInstanceOf[SyndEntry]).foreach { e =>
      val contents = e.getContents.map(_.asInstanceOf[SyndContent].getValue).mkString("\n")
      articleStorage ! ArticleStorage.StoreArticle(Article(url, e.getUri, e.getLink, e.getTitle, e.getAuthor, e.getPublishedDate, e.getUpdatedDate, contents))
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
