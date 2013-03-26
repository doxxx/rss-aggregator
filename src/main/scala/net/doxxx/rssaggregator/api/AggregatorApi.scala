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

trait AggregatorApi extends HttpService {
  implicit val feedFormat = jsonFormat4(Feed.apply)

  val aggregatorRef: ActorRef

  val aggregatorApiRoute = {
    get {
      path("") {
        respondWithMediaType(`text/html`) {
          complete(index)
        }
      } ~
      path("GetAllFeeds") {
        implicit val timeout = Timeout(30.seconds)
        respondWithMediaType(`application/json`) {
          complete {
            (aggregatorRef ? Aggregator.GetAllFeeds).mapTo[Seq[Feed]]
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
        <li>GetFeedList</li>
      </ul>
    </body>
  </html>

}

class AggregatorApiActor(val aggregatorRef: ActorRef) extends Actor with AggregatorApi {

  implicit def actorRefFactory = context

  def receive = runRoute(aggregatorApiRoute)
}
