package net.doxxx.rssaggregator

import akka.actor.{Props, ActorSystem}
import api.HttpApiActor
import akka.event.Logging
import spray.can.Http
import akka.io.IO

object Main extends App {
  implicit lazy val system = ActorSystem("rss-aggregator")

  val log = Logging(system, this.getClass)

  val aggregatorService = system.actorOf(Props[AggregatorService], name = "aggregator-service")
  aggregatorService ! AggregatorService.Start

  val userService = system.actorOf(Props(new UserService(aggregatorService)), name = "user-service")
  userService ! UserService.Start

  val httpApi = system.actorOf(Props(new HttpApiActor(userService)), name = "http-api")
  IO(Http) ! Http.Bind(httpApi, interface="localhost", port=8080)
}
