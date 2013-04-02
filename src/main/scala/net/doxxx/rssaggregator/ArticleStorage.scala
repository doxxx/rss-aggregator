package net.doxxx.rssaggregator

import akka.actor.{ActorLogging, Actor}
import akka.event.Logging
import com.mongodb.casbah.Imports._
import scala.concurrent._
import model._


class ArticleStorage extends Actor with ActorLogging {
  import ArticleStorage._
  import context.dispatcher

  val mongoClient = MongoClient()
  val db = mongoClient("rss-aggregator")
  val articles = db("articles")

  def receive = {
    case StoreArticle(article) => {
      if (articles.find(MongoDBObject("_id" -> article.uri)).isEmpty) {
        log.debug("Storing article {} -> {}", article.feedLink, article.subject)
        articles.save(article.toDBObject)
      }
    }
    case GetFeedArticles(feedLink) => {
      sender ! articles.find(MongoDBObject("feedLink" -> feedLink)).map(Article.fromDBObject(_))
    }
  }
}

object ArticleStorage {
  case class StoreArticle(article: Article)
  case class GetFeedArticles(feedLink: String)
}
