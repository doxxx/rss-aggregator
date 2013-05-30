package net.doxxx.rssaggregator

import spray.http._
import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.io.SyndFeedInput
import java.io.StringReader
import akka.io.IO
import spray.can.Http
import spray.client.pipelining._
import scala.concurrent._
import net.doxxx.rssaggregator.model.{Article, Feed, DAO}
import scala.annotation.tailrec
import spray.http.HttpResponse
import akka.actor.ActorSystem
import akka.event.Logging
import akka.util.Timeout
import scala.concurrent.duration._

/**
 * Created 13-05-23 10:20 PM by gordon.
 */
class FeedFetcher(actorSystem: ActorSystem) {
  import StatusCodes._
  import HttpHeaders._

  val log = Logging(actorSystem, this.getClass)
  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout = Timeout(120.seconds)
  val httpClient = IO(Http)(actorSystem)
  val pipeline = sendReceive(httpClient)

  def entity2SyndFeed(entity: HttpEntity): SyndFeed = new SyndFeedInput().build(new StringReader(entity.asString))

  private def parseFeed(url: String, entity: HttpEntity) = future {
    val sf = entity2SyndFeed(entity)
    log.debug("Parsed feed {}", sf.getTitle)
    val (feed, articles) = DAO.fromSyndFeed(url, sf)
    log.debug("Fetched feed {} containing {} articles", feed.title, articles.length)
    (feed.copy(lastFetchTime = Some(new java.util.Date())), articles)
  }

  private def moved(location: Option[Location]) = {
    location match {
      case Some(Location(uri)) => apply(uri.toString()) // restart fetch with new uri as the canonical url
      case None => Future.failed(new IllegalStateException("URL redirect with no Location header"))
    }
  }

  private def found(url: String, location: Option[Location]) = {
    location match {
      case Some(Location(uri)) => pipeline(Get(uri)) flatMap responseHandler(url)
      case None => Future.failed(new IllegalStateException("URL redirect with no Location header"))
    }
  }

  private def responseHandler(url: String)(response: HttpResponse): Future[(Feed, Seq[Article])] = {
    response.status match {
      case MovedPermanently | PermanentRedirect => moved(response.header[Location])
      case Found            | TemporaryRedirect => found(url, response.header[Location])
      case sc if (sc.isFailure)                 => Future.failed(new FeedNotFound(url, sc.reason))
      case _                                    => parseFeed(url, response.entity)
    }
  }

  def apply(url: String): Future[(Feed, Seq[Article])] = {
    pipeline(Get(url)) flatMap responseHandler(url)
  }

  class FeedNotFound(url: String, reason: String) extends RuntimeException(url + ": " + reason)
}
