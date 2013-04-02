package net.doxxx.rssaggregator.api

import akka.actor.{ActorRef, Actor}
import spray.util.SprayActorLogging

/**
 * Created 13-03-26 5:41 PM by gordon.
 */
class HttpApiActor(val aggregatorRef: ActorRef)
  extends Actor
  with SprayActorLogging
  with AggregatorApi
  with GoogleReaderApi {

  implicit def actorRefFactory = context
  def receive = runRoute(aggregatorApiRoute ~ googleReaderApiRoute)
}
