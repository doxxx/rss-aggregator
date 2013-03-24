package net.doxxx.rssaggregator

import akka.actor.{Props, Actor}
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import model.Feed
import scala.concurrent.duration._

class Aggregator extends Actor {
  import Aggregator._
  import context.dispatcher

  val log = Logging(context.system, this)

  val feedLoader = context.actorOf(Props[FeedLoader], "feed-loader")
  val feedStorage = context.actorOf(Props[FeedStorage], "feed-storage")

  def receive = {
    case AddFeed(url) => {
      implicit val timeout = Timeout(30.seconds)
      feedLoader ? FeedLoader.LoadFeed(url) recover {
        case t: Throwable => {
          log.error(t, "Could not load feed {}", url)
        }
      } map {
        case FeedLoader.Result(syndFeed) => {
          log.info("Loaded feed {} containing {} articles", syndFeed.getTitle, syndFeed.getEntries.size())
          FeedStorage.StoreFeed(url, Feed(feedLink = url.toString, link = syndFeed.getLink, title = syndFeed.getTitle,
            description = Option(syndFeed.getDescription))
          )
        }
      } pipeTo feedStorage
    }
    case Stop => {
      context.stop(self)
    }
  }
}

object Aggregator {
  case class AddFeed(url: String)
  case object Stop
}
