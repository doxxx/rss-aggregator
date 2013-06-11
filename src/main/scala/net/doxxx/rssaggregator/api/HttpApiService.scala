package net.doxxx.rssaggregator.api

import scala.language.postfixOps
import net.doxxx.rssaggregator.model._
import akka.actor._
import akka.pattern._
import spray.http._
import MediaTypes._
import spray.routing.authentication.{HttpAuthenticator, BasicAuth, UserPass}
import spray.routing._
import spray.util.SprayActorLogging
import spray.json.DefaultJsonProtocol._
import spray.httpx.SprayJsonSupport._
import scala.concurrent.duration._
import scala.concurrent._
import net.doxxx.rssaggregator.UserService
import scala.util.{Try, Failure, Success}

/**
 * Created 13-03-26 5:41 PM by gordon.
 */
class HttpApiService extends HttpServiceActor with SprayActorLogging {

  def receive = runRoute(googleReaderApiRoute)

  private implicit val executionContext = context.dispatcher
  private implicit val timeout = akka.util.Timeout(60.seconds)
  private val apiPath = "reader" / "api" / "0"
  private var userService: ActorRef = _

  override def preStart() {
    userService = context.actorFor(context.system.settings.config.getString("user-service-path"))
  }

  import JsonFormats._

  private def tag2category(tag: String) = Map("id" -> tag, "label" -> tag.substring(tag.lastIndexOf('/')+1))

  val auth: HttpAuthenticator[User] = BasicAuth(authenticator _, "rss-aggregator")

  private lazy val googleReaderApiRoute = {
    pathPrefix(apiPath) {
      authenticate(auth) { user =>
        pathPrefix("subscription") {
          path("list") {
            get {
              parameter("output") { output: String =>
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
            }
          } ~
          path("quickadd") {
            post {
              parameters("quickadd") { feedLink: String =>
                complete {
                  (userService ? UserService.Subscribe(user, feedLink, None, None)).map(result)
                }
              }
            }
          } ~
          path("edit") {
            post {
              parameters("ac" ! "subscribe", "s", "a", "t") { (feedLink: String, folder: String, title: String) =>
                complete {
                  (userService ? UserService.Subscribe(user, feedLink, Some(folder), Some(title))).map(result)
                }
              } ~
              parameters("ac" ! "unsubscribe", "s") { feedLink: String =>
                complete {
                  (userService ? UserService.Unsubscribe(user, feedLink)).map(result)
                }
              } ~
              parameters("ac" ! "edit", "s", "r"?, "a"?, "t"?) { (feedLink: String, removeTag: Option[String],
                                                                  addTag: Option[String], newTitle: Option[String]) =>
                complete {
                  (userService ? UserService.EditSubscription(user, feedLink, removeTag, addTag, newTitle)).map(result)
                }
              }
            }
          }
        } ~
        path("tag" / "list") {
          get {
            parameter("output") { output =>
              todo
            }
          }
        } ~
        path("unread-count") {
          get {
            parameter("output") { output =>
              todo
            }
          }
        } ~
        path("user-info") {
          get {
            todo
          }
        } ~
        path("edit-tag") {
          post {
            parameters("ac" ! "edit", "a", "s") { (folder: String, feedLink: String) =>
              todo
            } ~
            parameters("ac" ! "edit-tags", "a" ! "user/-/state/com.google/read", "async" ! "true", "i", "s"?) {
              (entryID: String, feed: Option[String]) =>
                todo
            } ~
            parameters("ac" ! "edit-tags", "r" ! "user/-/state/com.google/read", "async" ! "true", "i", "s"?) {
              (entryID: String, feed: Option[String]) =>
                todo
            }
          }
        } ~
        path("disable-tag") {
          post(parameters("ac" ! "disable-tags", "s", "t") { (feedLink: String, folder: String) =>
            todo
          })
        } ~
        path("mark-all-as-read") {
          post {
            parameters("s", "ts".as[Long]) { (feedLink: String, timestamp: Long) =>
              todo
            } ~
            parameters("t", "ts".as[Long]) { (feedLink: String, timestamp: Long) =>
              todo
            }
          }
        } ~
        path("import-opml") {
          post {
            entity(as[String]) { e =>
              complete {
                (userService ? UserService.ImportOpml(user, e)).mapTo[Map[String,Try[_]]].map(_.mapValues(result(_)))
              }
            }
          }
        }
      }
    } ~
    path("reader" / "atom" / "feed" / Rest) { feed: String =>
      authenticate(auth) { user =>
        get {
          parameter("n".as[Int] ?, "xt"?, "c"?) { (num: Option[Int], excludeTag: Option[String],
                                                   continuation: Option[String]) =>
            complete {
              val articlesFuture = (userService ? UserService.FetchArticles(user, feed, num, excludeTag, continuation)).mapTo[Seq[Article]]
              // TODO: Map articles to correct response format
              articlesFuture.map {
                articles => articles.map {
                  article => article.subject
                }
              }
            }
          }
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

  def todo = complete("TODO\n")

  def result(r: Any) = r match {
    case Success(_) => Map("result" -> "success")
    case Failure(cause) => Map("result" -> "failure", "reason" -> cause.toString)
    case other => sys.error("Invalid response from UserService: " + other.toString)
  }

}
