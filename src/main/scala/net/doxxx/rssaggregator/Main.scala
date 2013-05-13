package net.doxxx.rssaggregator

import akka.actor.{Props, ActorSystem}
import api.HttpApiActor
import akka.event.Logging
import spray.can.server.SprayCanHttpServerApp

object Main extends App with SprayCanHttpServerApp {
  implicit override lazy val system = ActorSystem("rss-aggregator")

  val log = Logging(system, this.getClass)

  val aggregatorService = system.actorOf(Props[AggregatorService], name = "aggregator-service")
  aggregatorService ! AggregatorService.Start

  val userService = system.actorOf(Props(new UserService(aggregatorService)), name = "user-service")
  userService ! UserService.Start

  val httpApi = system.actorOf(Props(new HttpApiActor(userService)), "http-api")
  newHttpServer(httpApi) ! Bind(interface = "localhost", port = 8080)
}
