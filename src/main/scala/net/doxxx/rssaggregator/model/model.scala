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
  private[model] val usersColl = db("users")

}

case class Feed(@Key("_id") link: String,
                siteLink: String,
                title: String,
                description: Option[String] = None,
                tags: Set[String] = Set.empty,
                lastFetchTime: Option[Date] = None)

object Feed {
  def fromSyndFeed(url: String, sf: SyndFeed): Feed = Feed(url, sf.getLink, sf.getTitle, Option(sf.getDescription))
}

object FeedDAO extends SalatDAO[Feed, String](collection = Model.feedsColl) {
  def findAll = find(MongoDBObject())
}

case class Article(_id: String,
                   feedLink: String,
                   link: Option[String],
                   subject: String,
                   author: String,
                   publishedDate: Option[Date],
                   updatedDate: Option[Date],
                   tags: Set[String] = Set.empty,
                   body: String)

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
    Article(id, feedLink, link, entry.getTitle, entry.getAuthor, Option(entry.getPublishedDate),
      Option(entry.getUpdatedDate), Set.empty, contents)
  }

  def make(feedLink: String, entry: SyndEntry): Article = {
    make(makeId(feedLink, entry), feedLink, entry)
  }
}

object DAO {
  def fromSyndFeed(url: String, sf: SyndFeed): (Feed, Seq[Article]) = {
    val feed = Feed.fromSyndFeed(url, sf)
    val syndEntries = sf.getEntries.map(_.asInstanceOf[SyndEntry]).toSeq
    val articles = syndEntries.map(e => Article.make(url, e))
    (feed, articles)
  }
}

case class Subscription(feedLink: String, title: String, tags: Set[String] = Set.empty, readArticles: Set[String] = Set.empty) {
  def setTitle(newTitle: String) = copy(title = newTitle)
  def addTag(tag: String) = copy(tags = tags + tag)
  def removeTag(tag: String) = copy(tags = tags - tag)
}

case class User(@Key("_id") email: String, password: String, subscriptions: Set[Subscription] = Set.empty) {
  def addSubscription(sub: Subscription) = copy(subscriptions = subscriptions + sub)
  def removeSubscription(sub: Subscription) = copy(subscriptions = subscriptions - sub)
}

object UserDAO extends SalatDAO[User, String](collection = Model.usersColl)
