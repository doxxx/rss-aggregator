package net.doxxx.rssaggregator

import akka.actor.{Props, Actor}
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import model.{Article, Feed}
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import com.sun.syndication.feed.synd.{SyndContent, SyndEntry}
import util.{Failure, Success}

class Aggregator extends Actor {
  import Aggregator._
  import context.dispatcher

  val log = Logging(context.system, this)

  val feedFetcher = context.actorOf(Props[FeedFetcher], "feed-fetcher")
  val feedStorage = context.actorOf(Props[FeedStorage], "feed-storage")
  val articleStorage = context.actorOf(Props[ArticleStorage], "article-storage")

  implicit val timeout = Timeout(30.seconds)

  def receive = {
    case Start => {
      log.info("Loading known feeds")
      (feedStorage ? FeedStorage.GetAllFeeds).mapTo[Seq[Feed]].onSuccess {
        case feeds: Seq[Feed] => feeds.foreach { f =>
          self ! AddFeed(f.link)
        }
      }
    }
    case GetAllFeeds => {
      feedStorage ? FeedStorage.GetAllFeeds pipeTo sender
    }
    case GetFeedArticles(feedLink) => {
      articleStorage ? ArticleStorage.GetFeedArticles(feedLink) pipeTo sender
    }
    case AddFeed(url) => {
      (feedFetcher ? FeedFetcher.FetchFeed(url)).mapTo[FeedFetcher.Result] onComplete {
        case Success(FeedFetcher.Result(syndFeed)) => {
          log.info("Fetched feed {} containing {} articles", syndFeed.getTitle, syndFeed.getEntries.size())
          // store feed in db
          feedStorage ! FeedStorage.StoreFeed(url, Feed(url.toString, syndFeed.getLink,syndFeed.getTitle, Option(syndFeed.getDescription)))
          // store feed articles in db
          syndFeed.getEntries.map(_.asInstanceOf[SyndEntry]).foreach { e =>
            val contents = e.getContents.map(_.asInstanceOf[SyndContent].getValue).mkString("\n")
            articleStorage ! ArticleStorage.StoreArticle(Article(url, e.getUri, e.getLink, e.getTitle, e.getAuthor, e.getPublishedDate, e.getUpdatedDate, contents))
          }
        }
        case Failure(t) => {
          log.error(t, "Could not load feed {}", url)
        }
      }

      // schedule future check
      context.system.scheduler.scheduleOnce(1.hour, self, AddFeed(url))
    }
    case Stop => {
      context.stop(self)
    }
  }
}

object Aggregator {
  case object Start
  case object GetAllFeeds
  case class GetFeedArticles(feedLink: String)
  case class AddFeed(url: String)
  case object Stop
}
