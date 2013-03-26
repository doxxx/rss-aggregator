package net.doxxx.rssaggregator.api

import spray.routing.HttpService
import akka.actor.ActorRef

/**
 * Created 13-03-26 5:36 PM by gordon.
 */
trait GoogleReaderApi extends HttpService {
  val aggregatorRef: ActorRef

  private val basePath = "reader/api/0"

  val googleReaderApiRoute = {
    get {
      path(basePath) {
        complete("TODO: Google Reader API")
      }
    }
  }
}
