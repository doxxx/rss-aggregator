package net.doxxx.rssaggregator

import akka.actor.{Props, Actor}
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import model.{Article, Feed}
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import com.sun.syndication.feed.synd.{SyndContent, SyndEntry}

class Aggregator extends Actor {
  import Aggregator._
  import context.dispatcher

  val log = Logging(context.system, this)

  val feedLoader = context.actorOf(Props[FeedLoader], "feed-loader")
  val feedStorage = context.actorOf(Props[FeedStorage], "feed-storage")
  val articleStorage = context.actorOf(Props[ArticleStorage], "article-storage")

  def receive = {
    case Start => {
      log.info("Starting aggregator")
      // TODO: Load list of feeds from db, check for new posts and schedule future checks
    }
    case GetAllFeeds => {
      implicit val timeout = Timeout(30.seconds)
      feedStorage ? FeedStorage.GetAllFeeds pipeTo sender
    }
    case AddFeed(url) => {
      implicit val timeout = Timeout(30.seconds)
      feedLoader ? FeedLoader.LoadFeed(url) recover {
        case t: Throwable => {
          log.error(t, "Could not load feed {}", url)
        }
      } onSuccess {
        case FeedLoader.Result(syndFeed) => {
          log.info("Loaded feed {} containing {} articles", syndFeed.getTitle, syndFeed.getEntries.size())
          feedStorage ! FeedStorage.StoreFeed(url, Feed(feedLink = url.toString, link = syndFeed.getLink,
            title = syndFeed.getTitle, description = Option(syndFeed.getDescription)))
          syndFeed.getEntries.map(_.asInstanceOf[SyndEntry]).foreach { e =>
            val contents = e.getContents.map(_.asInstanceOf[SyndContent].getValue).mkString("\n")
            articleStorage ! ArticleStorage.StoreArticle(Article(url, e.getUri, e.getLink, e.getTitle, e.getAuthor, contents))
          }
        }
      }
    }
    case Stop => {
      context.stop(self)
    }
  }
}

object Aggregator {
  case object Start
  case object GetAllFeeds
  case class AddFeed(url: String)
  case object Stop
}
