package net.doxxx.rssaggregator

import akka.actor.Actor
import akka.event.Logging
import com.sun.syndication.feed.synd.SyndFeed
import com.mongodb.casbah.Imports._
import model._

class FeedStorage extends Actor {
  import FeedStorage._
  import context.dispatcher

  val log = Logging(context.system, this)

  val mongoClient = MongoClient()
  val db = mongoClient("rss-aggregator")
  val feeds = db("feeds")

  def receive = {
    case StoreFeed(url, feed) => {
      log.info("Storing feed {}", feed.title)
      feeds.save(feed.toDBObject)
      sender ! Result(feed)
    }
  }
}

object FeedStorage {
  case class StoreFeed(url: String, feed: Feed)
  case class Result(feed: Feed)
}
