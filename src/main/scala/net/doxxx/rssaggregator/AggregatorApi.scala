package net.doxxx.rssaggregator

import akka.actor.Actor
import spray.routing.HttpService
import spray.http._
import MediaTypes._

trait AggregatorApi extends HttpService {
  val aggregatorApiRoute = {
    get {
      path("") {
        respondWithMediaType(`text/html`) {
          complete(index)
        }
      }
    }
  }

  lazy val index = <html>
    <body>
      <h1>rss-aggregator</h1>
      <p>Defined methods:</p>
      <ul>
        <li></li>
      </ul>
    </body>
  </html>
}

class AggregatorApiActor extends Actor with AggregatorApi {

  implicit def actorRefFactory = context

  def receive = runRoute(aggregatorApiRoute)
}
