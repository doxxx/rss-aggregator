package net.doxxx.rssaggregator.api

import akka.actor.{ActorRef, Actor}
import akka.pattern._
import spray.routing.HttpService
import spray.http._
import MediaTypes._
import spray.json._
import akka.util.Timeout
import scala.concurrent.duration._
import DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._
import net.doxxx.rssaggregator.model._
import net.doxxx.rssaggregator.Aggregator
import reflect.ClassTag
import akka.event.LoggingAdapter
import util.{Failure, Success, Try}

// hack for bug in akka 2.1 and scala 2.10

trait AggregatorApi extends HttpService {
  import AggregatorJsonProtocol._

  val log: LoggingAdapter
  val aggregatorRef: ActorRef
  implicit val timeout = Timeout(30.seconds)

  private val basePath = "aggregator/api/0"

  val aggregatorApiRoute = {
    get {
      path(basePath) {
        respondWithMediaType(`text/html`) {
          complete(index)
        }
      } ~
      path(basePath / "list-feeds") {
        respondWithMediaType(`application/json`) {
          complete {
            (aggregatorRef ? Aggregator.GetAllFeeds).mapTo[Seq[Feed]]
          }
        }
      } ~
      path(basePath / "list-articles") {
        parameter("feedLink") { feedLink: String =>
          respondWithMediaType(`application/json`) {
            complete {
              (aggregatorRef ? Aggregator.GetFeedArticles(feedLink)).mapTo[Iterator[Article]].map(_.toSeq)
            }
          }
        }
      }
    } ~
    post {
      path(basePath / "add-feed") {
        formField("url") { url: String =>
          respondWithMediaType(`application/json`) {
            complete {
              (aggregatorRef ? Aggregator.AddFeed(url)).mapTo[String].map {
                title => Map("success" -> title)
              }.recover {
                case t => Map("error" -> t.toString)
              }
            }
          }
        }
      } ~
      path(basePath / "import-opml") {
        entity(as[String]) { opml: String =>
          complete {
            log.info("Importing OPML...")
            aggregatorRef ! Aggregator.ImportOpml(opml)
            ""
          }
        }
      }
    }
  }

  lazy val index = <html>
    <body>
      <h1>rss-aggregator</h1>
      <p>Defined methods:</p>
      <ul>
        <li>GET list-feeds</li>
        <li>GET list-articles?feedLink=URL</li>
        <li>POST add-feed; body: url=URL</li>
      </ul>
    </body>
  </html>

}
