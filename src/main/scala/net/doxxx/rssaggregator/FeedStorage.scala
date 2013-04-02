package net.doxxx.rssaggregator

import akka.actor.Actor
import akka.event.Logging
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
    case GetAllFeeds => {
      sender ! feeds.find().map(Feed.fromDBObject(_)).toSeq
    }
    case StoreFeed(feed) => {
      if (feeds.find(MongoDBObject("_id" -> feed.link)).isEmpty) {
        log.info("Storing feed {}", feed.title)
        feeds.save(feed.toDBObject)
      }
    }
  }
}

object FeedStorage {
  case object GetAllFeeds
  case class StoreFeed(feed: Feed)
}
