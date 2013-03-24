package net.doxxx.rssaggregator

import akka.actor.Actor
import akka.event.Logging
import com.mongodb.casbah.Imports._
import scala.concurrent._
import model._


class ArticleStorage extends Actor {
  import ArticleStorage._
  import context.dispatcher

  val log = Logging(context.system, this)

  val mongoClient = MongoClient()
  val db = mongoClient("rss-aggregator")
  val articles = db("articles")

  def receive = {
    case StoreArticle(article) => {
      if (articles.find(MongoDBObject("_id" -> article.uri)).isEmpty) {
        log.info("Storing article {} -> {}", article.feedLink, article.subject)
        articles.save(article.toDBObject)
      }
    }
  }
}

object ArticleStorage {
  case class StoreArticle(article: Article)
}
