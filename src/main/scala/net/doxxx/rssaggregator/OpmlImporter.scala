package net.doxxx.rssaggregator

import akka.actor._
import model.Feed
import xml.XML
import java.io.StringReader

/**
 * Created 13-04-01 8:16 AM by gordon.
 */
class OpmlImporter extends Actor {
  import OpmlImporter._

  def receive = {
    case Import(opml) => {
      val root = XML.load(new StringReader(opml))
      val folders = root \ "body" \ "outline"
      val feeds = folders.flatMap { elem =>
        val folderName = (elem \ "@title").text
        val feeds = elem \ "outline"
        feeds map { elem =>
          val feedLink = (elem \ "@xmlUrl").text
          val siteLink = (elem \ "@htmlUrl").text
          val title = (elem \ "@title").text
          Feed(feedLink, siteLink, title, None, Set(folderName))
        }
      }
      sender ! feeds
    }
  }
}

object OpmlImporter {
  case class Import(opml: String)
}