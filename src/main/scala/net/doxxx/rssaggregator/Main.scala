package net.doxxx.rssaggregator

import akka.actor.{Props, ActorSystem}
import scala.concurrent.duration._
import akka.event.Logging

object Main extends App {
  import Aggregator._

  implicit val system = ActorSystem("rss-aggregator")
  val log = Logging(system, this.getClass)

  val aggregator = system.actorOf(Props[Aggregator], name = "aggregator")

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

  // shutdown after 30 seconds, so I don't create a new process every time I test the app
  Thread.sleep(30.seconds.toMillis)
  system.shutdown()
}
