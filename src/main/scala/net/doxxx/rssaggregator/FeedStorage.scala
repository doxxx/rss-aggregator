package net.doxxx.rssaggregator

import akka.actor.{ActorLogging, Actor}
import akka.event.Logging
import com.mongodb.casbah.Imports._
import model._

class FeedStorage extends Actor with ActorLogging {
  import FeedStorage._
  import context.dispatcher

  val mongoClient = MongoClient()
  val db = mongoClient("rss-aggregator")
  val feeds = db("feeds")

  def receive = {
    case GetAllFeeds => {
      sender ! feeds.find().map(Feed.fromDBObject(_)).toSeq
    }
    case StoreFeed(feed) => {
      if (feeds.find(MongoDBObject("_id" -> feed.link)).isEmpty) {
        log.debug("Storing feed {}", feed.title)
        feeds.save(feed.toDBObject)
      }
    }
  }
}

object FeedStorage {
  case object GetAllFeeds
  case class StoreFeed(feed: Feed)
}
