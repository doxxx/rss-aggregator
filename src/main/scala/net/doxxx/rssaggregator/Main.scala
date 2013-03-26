package net.doxxx.rssaggregator

import akka.actor.{Props, ActorSystem}
import api.HttpApiActor
import akka.event.Logging
import spray.can.server.SprayCanHttpServerApp

object Main extends App with SprayCanHttpServerApp {
  import Aggregator._

  implicit override lazy val system = ActorSystem("rss-aggregator")

  val log = Logging(system, this.getClass)

  val aggregator = system.actorOf(Props[Aggregator], name = "aggregator")
  aggregator ! Start

  val aggregatorApi = system.actorOf(Props(new HttpApiActor(aggregator)), "http-api")
  newHttpServer(aggregatorApi) ! Bind(interface = "localhost", port = 8080)

/*
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/features")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/technology-lab")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/gadgets")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/business")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/security")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/tech-policy")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/apple")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/gaming")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/science")
  aggregator ! AddFeed("http://feeds.arstechnica.com/arstechnica/staff-blogs")
*/

}
