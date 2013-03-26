package net.doxxx.rssaggregator

import com.mongodb.casbah.Imports._

package object model {

  case class Feed(link: String, siteLink: String, title: String, description: Option[String] = None) {
    def toDBObject = MongoDBObject(
      "_id" -> link,
      "siteLink" -> siteLink,
      "title" -> title,
      "description" -> description
    )
    def updateDBObject(dbo: MongoDBObject) {
      dbo.put("_id", link)
      dbo.put("siteLink", siteLink)
      dbo.put("title", title)
      dbo.put("description", description)
    }
  }

  object Feed {
    def fromDBObject(dbo: MongoDBObject): Feed = Feed(
      link = dbo.getAs[String]("_id").get,
      siteLink = dbo.getAs[String]("siteLink").get,
      title = dbo.getAs[String]("title").get,
      description = dbo.getAs[String]("description")
    )
  }

  case class Article(feedLink: String, uri: String, link: String, subject: String, author: String, body: String) {
    def toDBObject = MongoDBObject(
      "_id" -> uri,
      "feedLink" -> feedLink,
      "link" -> link,
      "subject" -> subject,
      "author" -> author,
      "body" -> body
    )
    def updateDBObject(dbo: MongoDBObject) {
      // TODO
    }
  }

  object Article {
    def fromDBObject(dbo: MongoDBObject): Article = Article(
      feedLink = dbo.getAs[String]("feedLink").get,
      uri = dbo.getAs[String]("uri").get,
      link = dbo.getAs[String]("link").get,
      subject = dbo.getAs[String]("subject").get,
      author = dbo.getAs[String]("author").get,
      body = dbo.getAs[String]("body").get
    )
  }

}
