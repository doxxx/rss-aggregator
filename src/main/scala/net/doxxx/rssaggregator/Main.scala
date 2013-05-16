package net.doxxx.rssaggregator

import akka.actor.{Props, ActorSystem}
import api.HttpApiActor
import akka.event.Logging
import akka.io.IO
import spray.can.Http

object Main extends App {
  implicit val system = ActorSystem("rss-aggregator")

  val log = Logging(system, this.getClass)

  val aggregatorService = system.actorOf(Props[AggregatorService], name = "aggregator-service")
  aggregatorService ! AggregatorService.Start

  val userService = system.actorOf(Props(classOf[UserService], aggregatorService), name = "user-service")
  userService ! UserService.Start

  val httpApi = system.actorOf(Props(classOf[HttpApiActor], userService), "http-api")
  IO(Http) ! Http.Bind(httpApi, interface="localhost", port=8080)
}
