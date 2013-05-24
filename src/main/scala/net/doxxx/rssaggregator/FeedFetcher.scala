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
  val log = Logging(actorSystem, this.getClass)
  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout = Timeout(120.seconds)
  val httpClient = IO(Http)(actorSystem)
  val pipeline = sendReceive(httpClient)

  def entity2SyndFeed(entity: HttpEntity): SyndFeed = new SyndFeedInput().build(new StringReader(entity.asString))

  private def entityHandler(url: String, entity: HttpEntity) = future {
    val sf = entity2SyndFeed(entity)
    log.debug("Parsed feed {}", sf.getTitle)
    val (feed, articles) = DAO.fromSyndFeed(url, sf)
    log.debug("Fetched feed {} containing {} articles", feed.title, articles.length)
    (feed, articles)
  }

  @tailrec
  private def extractLocation(headers: List[HttpHeader]): Option[String] = {
    headers match {
      case Nil => None
      case HttpHeaders.Location(uri) :: rest => Some(uri.toString())
      case HttpHeader(_, _) :: rest => extractLocation(rest)
    }
  }

  private def permRedirectHandler(headers: List[HttpHeader]) = {
    extractLocation(headers) match {
      case Some(location) => apply(location)
      case None => Promise.failed(new IllegalStateException("URL redirect with no Location header")).future
    }
  }

  private def tempRedirectHandler(url: String, headers: List[HttpHeader]) = {
    extractLocation(headers) match {
      case Some(location) => pipeline(Get(location)) flatMap responseHandler(url)
      case None => Promise.failed(new IllegalStateException("URL redirect with no Location header")).future
    }
  }

  private def responseHandler(url: String)(r: HttpResponse): Future[(Feed, Seq[Article])] = r match {
    case HttpResponse(StatusCodes.OK, entity, _, _)                      => entityHandler(url, entity)
    case HttpResponse(StatusCodes.MovedPermanently, entity, headers, _)  => permRedirectHandler(headers)
    case HttpResponse(StatusCodes.PermanentRedirect, entity, headers, _) => permRedirectHandler(headers)
    case HttpResponse(StatusCodes.Found, entity, headers, _)             => tempRedirectHandler(url, headers)
    case HttpResponse(StatusCodes.TemporaryRedirect, entity, headers, _) => tempRedirectHandler(url, headers)
    case HttpResponse(sc, _, _, _) if (sc.isFailure) => Promise.failed(new FeedNotFound(url, sc.reason)).future
  }

  def apply(url: String): Future[(Feed, Seq[Article])] = {
    pipeline(Get(url)) flatMap responseHandler(url)
  }

  class FeedNotFound(url: String, reason: String) extends RuntimeException(url + ": " + reason)
}
