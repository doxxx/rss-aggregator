package net.doxxx.rssaggregator.api

import language.postfixOps
import akka.actor.ActorRef
import spray.routing._
import spray.http._
import MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.json._
import DefaultJsonProtocol._
import net.doxxx.rssaggregator.model._
import net.doxxx.rssaggregator.Aggregator

/**
 * Created 13-03-26 5:36 PM by gordon.
 */
trait GoogleReaderApi extends HttpService {
  val aggregatorRef: ActorRef

  private val apiPath = "reader/api/0"

  val googleReaderApiRoute = {
    get {
      path("/accounts/ClientLogin") {
        formFields("accountType", "Email", "Passwd", "service", "source")(login _)
      } ~
      path(apiPath / "subscription/list") {
        parameter("output")(subscriptionList _)
      } ~
      path(apiPath / "tag/list") {
        parameter("output")(tagList _)
      } ~
      path(apiPath / "unread-count") {
        parameter("output")(unreadCount _)
      } ~
      path(apiPath / "user-info")(userInfo)
      path("reader/atom/feed" / Rest) { feed: String =>
        parameter("n".as[Int]?, "xt"?, "c"?) { (n, xt, c) => getFeed(feed, n, xt, c) }
      }
    }
    post {
      path(apiPath / "subscription/edit") {
        parameters("ac" ! "subscribe", "s", "a", "t")(addSubscription _) ~
        parameters("ac" ! "unsubscribe", "s")(deleteSubscription _) ~
        parameters("ac" ! "edit", "s", "r"?, "a"?, "t"?)(moveRenameSubscription _)
      } ~
      path(apiPath / "edit-tag") {
        parameters("ac" ! "edit", "a", "s")(createFolder _) ~
        parameters("ac" ! "edit-tags", "a" ! "user/-/state/com.google/read", "async" ! "true", "i", "s"?)(markPostRead _) ~
        parameters("ac" ! "edit-tags", "r" ! "user/-/state/com.google/read", "async" ! "true", "i", "s"?)(markPostUnread _)
      } ~
      path(apiPath / "disable-tag") {
        parameters("ac" ! "disable-tags", "s", "t")(deleteFolder _)
      } ~
      path(apiPath / "mark-all-as-read") {
        parameters("s", "ts".as[Long])(markFeedAsRead _) ~
        parameters("t", "ts".as[Long])(markFolderAsRead _)
      }
    }
  }

  def login(accountType: String, email: String, passwd: String, service: String, source: String) = {
    respondWithMediaType(`text/plain`) {
      complete {
        // TODO: Mimic Google ClientLogin service
        "SID=...\nLSID=...\nAuth=...\n"
      }
    }
  }

  def subscriptionList(output: String) = {
    output match {
      case "json" => respondWithMediaType(`application/json`) {
        complete {
          "TODO"
        }
      }
      case "xml" => respondWithMediaType(`text/xml`) {
        complete {
          "TODO"
        }
      }
      case _ => reject(MalformedQueryParamRejection("invalid output: %s".format(output), "output"))
    }
  }

  def addSubscription(subscription: String, folder: String, title: String) = {
    complete("TODO")
  }

  def deleteSubscription(subscription: String) = {
    complete("TODO")
  }

  def moveRenameSubscription(subscription: String, oldFolder: Option[String], newFolder: Option[String], newTitle: Option[String]) = {
    complete(List(subscription, oldFolder, newFolder, newTitle).mkString(" "))
  }

  def createFolder(folder: String, subscription: String) = {
    complete("TODO")
  }

  def deleteFolder(folder: String, title: String) = {
    complete("TODO")
  }

  def tagList(output: String) = {
    complete("TODO")
  }

  def markFeedAsRead(subscription: String, timestamp: Long) = {
    complete("TODO")
  }

  def markFolderAsRead(folder: String, timestamp: Long) = {
    complete("TODO")
  }

  def markPostRead(entryID: String, feed: Option[String]) = {
    complete("TODO")
  }

  def markPostUnread(entryID: String, feed: Option[String]) = {
    complete("TODO")
  }

  def unreadCount(output: String) = {
    complete("TODO")
  }

  def getFeed(feed: String, numItems: Option[Int], excludeTags: Option[String], continuation: Option[String]) = {
    complete("TODO")
  }

  def userInfo = {
    complete("TODO")
  }
}
