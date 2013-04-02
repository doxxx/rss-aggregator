package net.doxxx.rssaggregator

import com.mongodb.casbah.Imports._
import java.util.Date

package object model {

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
  }

  case class Article(feedLink: String, uri: String, link: String, subject: String, author: String, publishedDate: Date,
                     updatedDate: Date, body: String) {
    def toDBObject = MongoDBObject(
      "_id" -> uri,
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
    def fromDBObject(dbo: MongoDBObject): Article = Article(
      uri = dbo.getAs[String]("_id").get,
      feedLink = dbo.getAs[String]("feedLink").get,
      link = dbo.getAs[String]("link").get,
      subject = dbo.getAs[String]("subject").get,
      author = dbo.getAs[String]("author").get,
      publishedDate = dbo.getAs[Date]("publishedDate").get,
      updatedDate = dbo.getAs[Date]("publishedDate").get,
      body = dbo.getAs[String]("body").get
    )
  }

}
