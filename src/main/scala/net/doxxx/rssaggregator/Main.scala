package net.doxxx.rssaggregator

import akka.actor.{Props, ActorSystem}
import api.HttpApiService
import akka.event.Logging
import spray.can.Http
import akka.io.IO
import java.net.{InetSocketAddress, NetworkInterface, InetAddress}
import scala.collection.JavaConversions._

object Main extends App {
  implicit lazy val system = ActorSystem("rss-aggregator")

  val log = Logging(system, this.getClass)

  val aggregatorService = system.actorOf(Props[AggregatorService], name = "aggregator-service")
  val userService = system.actorOf(Props(new UserService(aggregatorService)), name = "user-service")
  val httpApiService = system.actorOf(Props(new HttpApiService(userService)), name = "http-api-service")

  val allInterfaceAddresses = NetworkInterface.getNetworkInterfaces.toList.flatMap(_.getInterfaceAddresses).map(_.getAddress.getHostAddress)
  allInterfaceAddresses.foreach { addr =>
    IO(Http) ! Http.Bind(httpApiService, addr, 8080)
  }

}
