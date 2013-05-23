package net.doxxx.rssaggregator.api

import net.doxxx.rssaggregator.model._
import akka.actor.{ActorRef, Actor}
import akka.pattern._
import spray.http._
import MediaTypes._
import spray.routing.authentication.BasicAuth
import spray.routing.authentication.UserPass
import spray.routing.{HttpService, MalformedQueryParamRejection}
import spray.util.SprayActorLogging
import scala.concurrent.duration._
import scala.concurrent._
import net.doxxx.rssaggregator.UserService
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._
import scala.util.{Try, Failure, Success}
import spray.json.{JsNumber, JsValue, RootJsonFormat}
import java.util.Date

/**
 * Created 13-03-26 5:41 PM by gordon.
 */
class HttpApiActor(val userService: ActorRef)
  extends Actor
  with SprayActorLogging
  with HttpService {

  implicit def actorRefFactory = context

  private implicit val executionContext = context.dispatcher

  def receive = runRoute(googleReaderApiRoute)

  private val apiPath = "reader" / "api" / "0"

  private implicit val timeout = akka.util.Timeout(10.seconds)

  lazy val googleReaderApiRoute = {
    authenticate(BasicAuth(authenticator _, "rss-aggregator")) { implicit user =>
      get {
        path(apiPath / "subscription" / "list") {
          parameter("output")(subscriptionList _)
        } ~
          path(apiPath / "tag" / "list") {
            parameter("output")(tagList _)
          } ~
          path(apiPath / "unread-count") {
            parameter("output")(unreadCount _)
          } ~
          path(apiPath / "user-info")(userInfo) ~
          path("reader" / "atom" / "feed" / Rest) { feed: String =>
            parameter("n".as[Int]?, "xt"?, "c"?) { (n, xt, c) => getFeed(feed, n, xt, c) }
          }
      } ~
        post {
          path(apiPath / "subscription" / "quickadd") {
            parameters("quickadd")(quickAddSubscription _)
          } ~
            path(apiPath / "subscription" / "edit") {
              parameters("ac" ! "subscribe", "s", "a", "t")(addSubscription _) ~
                parameters("ac" ! "unsubscribe", "s")(deleteSubscription _) ~
                parameters("ac" ! "edit", "s", "r"?, "a"?, "t"?)(editSubscription _)
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

  def authenticator(userPass: Option[UserPass]): Future[Option[User]] = {
    userPass match {
      case Some(up) => (userService ? UserService.Authenticate(up.user, up.pass)).mapTo[Option[User]]
      case None => future { None }
    }
  }

  def subscriptionList(output: String)(implicit user: User) = {
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

  def quickAddSubscription(feedLink: String)(implicit user: User) = {
    complete {
      (userService ? UserService.Subscribe(user, feedLink, None, None)).map(jsonResult)
    }
  }

  def addSubscription(feedLink: String, folder: String, title: String)(implicit user: User) = {
    complete {
      (userService ? UserService.Subscribe(user, feedLink, Some(folder), Some(title))).map(jsonResult)
    }
  }

  def deleteSubscription(feedLink: String)(implicit user: User) = {
    complete {
      (userService ? UserService.Unsubscribe(user, feedLink)).map(jsonResult)
    }
  }

  def editSubscription(feedLink: String, removeTag: Option[String], addTag: Option[String], newTitle: Option[String])(implicit user: User) = {
    complete {
      (userService ? UserService.EditSubscription(user, feedLink, removeTag, addTag, newTitle)).map(jsonResult)
    }
  }

  def createFolder(folder: String, subscription: String)(implicit user: User) = {
    todo
  }

  def deleteFolder(folder: String, title: String)(implicit user: User) = {
    todo
  }

  def tagList(output: String)(implicit user: User) = {
    todo
  }

  def markFeedAsRead(subscription: String, timestamp: Long)(implicit user: User) = {
    todo
  }

  def markFolderAsRead(folder: String, timestamp: Long)(implicit user: User) = {
    todo
  }

  def markPostRead(entryID: String, feed: Option[String])(implicit user: User) = {
    todo
  }

  def markPostUnread(entryID: String, feed: Option[String])(implicit user: User) = {
    todo
  }

  def unreadCount(output: String)(implicit user: User) = {
    todo
  }

  def getFeed(feed: String, numItems: Option[Int], excludeTags: Option[String], continuation: Option[String])(implicit user: User) = {
    todo
  }

  def userInfo(implicit user: User) = {
    todo
  }

  def todo = complete("TODO\n")

  def jsonResult(r: Any) = r match {
    case Success(_) => Map("result" -> "success")
    case Failure(cause) => Map("result" -> "failure", "reason" -> cause.toString)
    case other => sys.error("Invalid response from UserService: " + other.toString)
  }

  implicit object jsonDateFormat extends RootJsonFormat[Date] {
    def write(obj: Date): JsValue = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(t) => new Date(t.toLongExact)
      case _ => throw new IllegalArgumentException("Date expected")
    }
  }
}
