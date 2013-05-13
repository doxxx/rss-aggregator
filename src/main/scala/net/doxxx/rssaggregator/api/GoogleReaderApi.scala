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
import scala.util.{Failure, Success, Try}

/**
 * Created 13-03-26 5:36 PM by gordon.
 */
trait GoogleReaderApi extends HttpService {
  val log: LoggingAdapter
  val aggregatorRef: ActorRef

  private val apiPath = "reader/api/0"

  private implicit val timeout = akka.util.Timeout(120.seconds)

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
          path(apiPath / "subscription/quickadd") {
            parameters("quickadd")(quickAddSubscription _)
          } ~
          path(apiPath / "subscription/edit") {
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
      case Some(up) => (aggregatorRef ? Authenticate(up.user, up.pass)).mapTo[Option[User]]
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
      (aggregatorRef ? AddFeed(feedLink)).mapTo[AddFeedResult].map {
        case AddFeedResult(feed) => {
          log.debug("AddFeed success: {}", feedLink)
          user.subscriptions.find { _.feedLink == feedLink } match {
            case Some(s) => "Already subscribed.\n"
            case None => {
              UserDAO.save(user.addSubscription(Subscription(feedLink, feed.title, Set.empty, Set.empty)))
              "Subscription added.\n"
            }
          }
        }
      }
    }
  }

  def addSubscription(feedLink: String, folder: String, title: String)(implicit user: User) = {
    complete {
      (aggregatorRef ? AddFeed(feedLink)).mapTo[AddFeedResult].map {
        case AddFeedResult(feed) => {
          user.subscriptions.find { _.feedLink == feedLink } match {
            case Some(s) => {
              if (!s.tags.contains(folder)) {
                UserDAO.save(user.removeSubscription(s).addSubscription(s.addTag(folder)))
                "Already subscribed. Added to folder.\n"
              }
            }
            case None => {
              UserDAO.save(user.addSubscription(Subscription(feedLink, title, Set(folder), Set.empty)))
              "Subscription added.\n"
            }
          }
        }
      }
    }
  }

  def deleteSubscription(feedLink: String)(implicit user: User) = {
    complete {
      future {
        user.subscriptions.find { _.feedLink == feedLink } match {
          case Some(sub) => {
            UserDAO.save(user.removeSubscription(sub))
            "Subscription removed.\n"
          }
          case None => {
            "Not subscribed.\n"
          }
        }
      }
    }
  }

  def editSubscription(feedLink: String, removeTag: Option[String], addTag: Option[String], newTitle: Option[String])(implicit user: User) = {
    complete {
      future {
        user.subscriptions.find { _.feedLink == feedLink } match {
          case Some(sub) => {
            var newSub = sub

            newSub = (removeTag, addTag) match {
              case (Some(oldTag), Some(newTag)) => newSub.removeTag(oldTag).addTag(newTag)
              case (Some(oldTag), None) => newSub.removeTag(oldTag)
              case (None, Some(newTag)) => newSub.addTag(newTag)
              case (None, None) => newSub
            }

            newSub = newTitle match {
              case Some(s) => newSub.setTitle(s)
              case None => newSub
            }

            UserDAO.save(user.removeSubscription(sub).addSubscription(newSub))
            "Subscription updated.\n"
          }
          case None => {
            "Not subscribed.\n"
          }
        }
      }
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
}
