package net.doxxx.rssaggregator.model

import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import java.util.Date
import com.sun.syndication.feed.synd._
import scala.collection.JavaConversions._

private object Model {

  private[model] val mongoClient = MongoClient()
  private[model] val db = mongoClient("rss-aggregator")
  private[model] val feedsColl = db("feeds")
  private[model] val articlesColl = db("articles")

}

case class Feed(@Key("_id") link: String, siteLink: String, title: String, description: Option[String] = None, tags: Set[String] = Set.empty)

object FeedDAO extends SalatDAO[Feed, String](collection = Model.feedsColl) {
  def findAll = find(MongoDBObject())
}

case class Article(_id: String, feedLink: String, link: Option[String], subject: String, author: String, publishedDate: Option[Date], updatedDate: Option[Date], body: String)

object ArticleDAO extends SalatDAO[Article, String](collection = Model.articlesColl) {
  def findByFeedLink(feedLink: String) = {
    find(MongoDBObject("feedLink" -> feedLink))
  }
}

object Article {
  def makeId(feedLink: String, entry: SyndEntry): String = {
    // both published and updated date could be null, use former over latter
    val link = Option(entry.getLink)
    val uri = Option(entry.getUri)
    val date = Option(entry.getPublishedDate).orElse(Option(entry.getUpdatedDate)).map(_.getTime.toString)
    assert(link.isDefined || uri.isDefined || date.isDefined)
    Seq(feedLink, link.getOrElse(""), uri.getOrElse(""), date.getOrElse("")).mkString("|")
  }

  def make(id: String, feedLink: String, entry: SyndEntry): Article = {
    val link = Option(entry.getLink).orElse(Option(entry.getUri))
    val contents = entry.getContents.map(_.asInstanceOf[SyndContent].getValue).mkString("\n")
    Article(id, feedLink, link, entry.getTitle, entry.getAuthor, Option(entry.getPublishedDate), Option(entry.getUpdatedDate), contents)
  }
}