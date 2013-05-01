package net.doxxx.rssaggregator.api

import language.postfixOps
import akka.actor.ActorRef
import akka.pattern._
import spray.routing._
import spray.http._
import MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.json._
import DefaultJsonProtocol._
import net.doxxx.rssaggregator.model._
import net.doxxx.rssaggregator.Aggregator
import Aggregator._
import akka.event.LoggingAdapter
import spray.routing.authentication.{UserPass, BasicAuth}
import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created 13-03-26 5:36 PM by gordon.
 */
trait GoogleReaderApi extends HttpService {
  val log: LoggingAdapter
  val aggregatorRef: ActorRef

  private val apiPath = "reader/api/0"

  private implicit val timeout = akka.util.Timeout(30.seconds)

  val googleReaderApiRoute = {
    authenticate(BasicAuth(authenticator _, "rss-aggregator")) { implicit user =>
      get {
        path(apiPath / "subscription/list") {
          parameter("output")(subscriptionList _)
        } ~
        path(apiPath / "tag/list") {
          parameter("output")(tagList _)
        } ~
        path(apiPath / "unread-count") {
          parameter("output")(unreadCount _)
        } ~
        path(apiPath / "user-info")(userInfo) ~
        path("reader/atom/feed" / Rest) { feed: String =>
          parameter("n".as[Int]?, "xt"?, "c"?) { (n, xt, c) => getFeed(feed, n, xt, c) }
        }
      } ~
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
  }

  def authenticator(userPass: Option[UserPass]): Future[Option[AuthenticatedUser]] = {
    userPass match {
      case Some(up) => (aggregatorRef ? Authenticate(up.user, up.pass)).mapTo[AuthenticatedUser].map(Some(_))
      case None => future { None }
    }
  }

  def subscriptionList(output: String)(implicit user: AuthenticatedUser) = {
    output match {
      case "json" => respondWithMediaType(`application/json`) {
        todo
      }
      case "xml" => respondWithMediaType(`text/xml`) {
        todo
      }
      case _ => reject(MalformedQueryParamRejection("invalid output: %s".format(output), "output"))
    }
  }

  def addSubscription(subscription: String, folder: String, title: String)(implicit user: AuthenticatedUser) = {
    todo
  }

  def deleteSubscription(subscription: String)(implicit user: AuthenticatedUser) = {
    todo
  }

  def moveRenameSubscription(subscription: String, oldFolder: Option[String], newFolder: Option[String], newTitle: Option[String])(implicit user: AuthenticatedUser) = {
    complete(List(subscription, oldFolder, newFolder, newTitle).mkString(" "))
  }

  def createFolder(folder: String, subscription: String)(implicit user: AuthenticatedUser) = {
    todo
  }

  def deleteFolder(folder: String, title: String)(implicit user: AuthenticatedUser) = {
    todo
  }

  def tagList(output: String)(implicit user: AuthenticatedUser) = {
    todo
  }

  def markFeedAsRead(subscription: String, timestamp: Long)(implicit user: AuthenticatedUser) = {
    todo
  }

  def markFolderAsRead(folder: String, timestamp: Long)(implicit user: AuthenticatedUser) = {
    todo
  }

  def markPostRead(entryID: String, feed: Option[String])(implicit user: AuthenticatedUser) = {
    todo
  }

  def markPostUnread(entryID: String, feed: Option[String])(implicit user: AuthenticatedUser) = {
    todo
  }

  def unreadCount(output: String)(implicit user: AuthenticatedUser) = {
    todo
  }

  def getFeed(feed: String, numItems: Option[Int], excludeTags: Option[String], continuation: Option[String])(implicit user: AuthenticatedUser) = {
    todo
  }

  def userInfo(implicit user: AuthenticatedUser) = {
    todo
  }
  
  def todo = complete("TODO\n")
}
