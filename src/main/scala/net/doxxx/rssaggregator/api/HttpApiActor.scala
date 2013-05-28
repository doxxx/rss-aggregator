package net.doxxx.rssaggregator.api

import scala.language.postfixOps
import net.doxxx.rssaggregator.model._
import akka.actor.{ActorRef, Actor}
import akka.pattern._
import spray.http._
import MediaTypes._
import spray.routing.authentication.BasicAuth
import spray.routing.authentication.UserPass
import spray.routing._
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
  extends HttpServiceActor
  with SprayActorLogging {

  def receive = runRoute(googleReaderApiRoute)

  private implicit val executionContext = context.dispatcher
  private implicit val timeout = akka.util.Timeout(60.seconds)
  private val apiPath = "reader" / "api" / "0"

  lazy val googleReaderApiRoute = {
    pathPrefix(apiPath) {
      authenticate(BasicAuth(authenticator _, "rss-aggregator")) { implicit user =>
        pathPrefix("subscription") {
          path("list") {
            get(parameter("output")(subscriptionList _))
          } ~
          path("quickadd") {
            post(parameters("quickadd")(quickAddSubscription _))
          } ~
          path("edit") {
            post {
              parameters("ac" ! "subscribe", "s", "a", "t")(addSubscription _) ~
              parameters("ac" ! "unsubscribe", "s")(deleteSubscription _) ~
              parameters("ac" ! "edit", "s", "r"?, "a"?, "t"?)(editSubscription _)
            }
          }
        } ~
        path("tag" / "list") {
          get(parameter("output")(tagList _))
        } ~
        path("unread-count") {
          get(parameter("output")(unreadCount _))
        } ~
        path("user-info"){
          get(userInfo)
        } ~
        path("edit-tag") {
          post {
            parameters("ac" ! "edit", "a", "s")(createFolder _) ~
            parameters("ac" ! "edit-tags", "a" ! "user/-/state/com.google/read", "async" ! "true", "i", "s"?)(markPostRead _) ~
            parameters("ac" ! "edit-tags", "r" ! "user/-/state/com.google/read", "async" ! "true", "i", "s"?)(markPostUnread _)
          }
        } ~
        path("disable-tag") {
          post(parameters("ac" ! "disable-tags", "s", "t")(deleteFolder _))
        } ~
        path("mark-all-as-read") {
          post {
            parameters("s", "ts".as[Long])(markFeedAsRead _) ~
            parameters("t", "ts".as[Long])(markFolderAsRead _)
          }
        }
      }
    } ~
    path("reader" / "atom" / "feed" / Rest) { feed: String =>
      authenticate(BasicAuth(authenticator _, "rss-aggregator")) { implicit user =>
        get(parameter("n".as[Int]?, "xt"?, "c"?) { (n, xt, c) => getFeed(feed, n, xt, c) })
      }
    }
  }

  def authenticator(userPass: Option[UserPass]): Future[Option[User]] = {
    userPass match {
      case Some(up) => (userService ? UserService.Authenticate(up.user, up.pass)).mapTo[Option[User]]
      case None => future { None }
    }
  }

  def tag2category(tag: String) = Map("id" -> tag, "label" -> tag.substring(tag.lastIndexOf('/')+1))

  case class GRSubscription(id: String, title: String, categories: Seq[Map[String,String]], sortid: String, firstitemmsec: String)

  implicit val subscriptionsFormat = jsonFormat5(GRSubscription)

  def subscriptionList(output: String)(implicit user: User) = {
    output match {
      case "json" => respondWithMediaType(`application/json`) {
        complete(user.subscriptions.toSeq.map { sub =>
          GRSubscription(sub.feedLink, sub.title, sub.tags.toSeq.map(tag2category), "12345678", "0")
        })
      }
      case "xml" => respondWithMediaType(`text/xml`) {
        reject(UnsupportedRequestContentTypeRejection("text/xml"))
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
