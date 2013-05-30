package net.doxxx.rssaggregator

import akka.actor.{Props, ActorSystem}
import api.HttpApiService
import akka.event.Logging
import spray.can.Http
import akka.io.IO
import java.net.{InetSocketAddress, NetworkInterface, InetAddress}
import scala.collection.JavaConversions._

object Main extends App {
  implicit val system = ActorSystem("rss-aggregator")
  val log = Logging(system, this.getClass)
  log.debug("Settings:\n{}", system.settings.toString)

  val aggregatorService = system.actorOf(Props[AggregatorService], name = "aggregator-service")
  val userService = system.actorOf(Props[UserService], name = "user-service")
  val httpApiService = system.actorOf(Props[HttpApiService], name = "http-api-service")

  val allInterfaceAddresses = NetworkInterface.getNetworkInterfaces.toList.flatMap(_.getInterfaceAddresses).map(_.getAddress.getHostAddress)
  allInterfaceAddresses.foreach { addr =>
    IO(Http) ! Http.Bind(httpApiService, addr, 8080)
  }

}
