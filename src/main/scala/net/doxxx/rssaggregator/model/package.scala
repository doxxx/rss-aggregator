package net.doxxx.rssaggregator

import com.mongodb.casbah.Imports._
import java.util.Date
import com.sun.syndication.feed.synd._

package object model {

  private val mongoClient = MongoClient()
  private val db = mongoClient("rss-aggregator")
  private val feedsColl = db("feeds")
  private val articlesColl = db("articles")

  case class Feed(link: String, siteLink: String, title: String, description: Option[String] = None, tags: Set[String] = Set.empty) {
    def toDBObject = MongoDBObject(
      "_id" -> link,
      "siteLink" -> siteLink,
      "title" -> title,
      "description" -> description,
      "tags" -> tags
    )
    def updateDBObject(dbo: MongoDBObject) {
      dbo.put("_id", link)
      dbo.put("siteLink", siteLink)
      dbo.put("title", title)
      dbo.put("description", description)
      dbo.put("tags", dbo.get("tags").toSet ++ tags)
    }
  }

  object Feed {
    def fromDBObject(dbo: MongoDBObject): Feed = Feed(
      link = dbo.getAs[String]("_id").get,
      siteLink = dbo.getAs[String]("siteLink").get,
      title = dbo.getAs[String]("title").get,
      description = dbo.getAs[String]("description"),
      tags = dbo.getAs[List[String]]("tags").getOrElse(Nil).toSet
    )

    def findAll: Seq[Feed] = {
      feedsColl.find().map(fromDBObject(_)).toSeq
    }

    def findByFeedLink(feedLink: String): Option[Feed] = {
      feedsColl.findOneByID(feedLink).map(dbo => fromDBObject(dbo))
    }

    def save(feed: Feed) {
      feedsColl.save(feed.toDBObject)
    }
  }

  case class Article(id: String, feedLink: String, link: String, subject: String, author: String, publishedDate: Date,
                     updatedDate: Date, body: String) {
    def toDBObject = MongoDBObject(
      "_id" -> id,
      "feedLink" -> feedLink,
      "link" -> link,
      "subject" -> subject,
      "author" -> author,
      "publishedDate" -> publishedDate,
      "updatedDate" -> updatedDate,
      "body" -> body
    )
    def updateDBObject(dbo: MongoDBObject) {
      // TODO
    }
  }

  object Article {
    def makeId(feedLink: String, entry: SyndEntry): String = {
      // both published and updated date could be null, use former over latter
      val date = Option(entry.getPublishedDate).orElse(Option(entry.getUpdatedDate)).map(_.getTime.toString).getOrElse("")
      Seq(feedLink, entry.getLink, entry.getUri, date).mkString("|")
    }

    def fromDBObject(dbo: MongoDBObject): Article = Article(
      id = dbo.getAs[String]("_id").get,
      feedLink = dbo.getAs[String]("feedLink").get,
      link = dbo.getAs[String]("link").get,
      subject = dbo.getAs[String]("subject").get,
      author = dbo.getAs[String]("author").get,
      publishedDate = dbo.getAs[Date]("publishedDate").get,
      updatedDate = dbo.getAs[Date]("publishedDate").get,
      body = dbo.getAs[String]("body").get
    )

    def findAll: Seq[Article] = {
      articlesColl.find().map(fromDBObject(_)).toSeq
    }

    def findByFeedLink(feedLink: String): Seq[Article] = {
      articlesColl.find(MongoDBObject("feedLink" -> feedLink)).map(fromDBObject(_)).toSeq
    }

    def findById(id: String): Option[Article] = {
      articlesColl.findOneByID(id).map(fromDBObject(_))
    }

    def save(article: Article) {
      articlesColl.save(article.toDBObject)
    }
  }

}
