package net.doxxx.rssaggregator

import akka.actor.Actor
import akka.event.Logging
import akka.pattern._
import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.io.SyndFeedInput
import java.io.InputStreamReader
import java.net.URL
import scala.concurrent._

class FeedLoader extends Actor {
  import FeedLoader._
  import context.dispatcher

  val log = Logging(context.system, this)

  def receive = {
    case LoadFeed(url) => {
      log.info("Loading feed {}", url)
      future {
        Result(new SyndFeedInput().build(new InputStreamReader(new URL(url).openStream())))
      } pipeTo sender
    }
  }
}

object FeedLoader {
  case class LoadFeed(url: String)
  case class Result(feed: SyndFeed)
}
