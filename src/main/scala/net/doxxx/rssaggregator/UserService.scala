package net.doxxx.rssaggregator

import akka.actor.{ActorRef, ActorLogging, Actor}
import akka.pattern._
import net.doxxx.rssaggregator.model._
import scala.concurrent._
import net.doxxx.rssaggregator.model.User
import net.doxxx.rssaggregator.model.Subscription
import scala.Some
import com.mongodb.casbah.commons.MongoDBObject
import akka.util.Timeout
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.event.LoggingReceive
import scala.xml.XML
import java.io.StringReader

/**
 * Created 13-05-13 8:18 AM by gordon.
 */
class UserService extends Actor with ActorLogging {
  import UserService._

  private implicit val executionContext = context.dispatcher
  private implicit val timeout = Timeout(10.seconds)

  private lazy val aggregatorService = context.actorSelection(context.system.settings.config.getString("aggregator-service-path"))

  def receive = LoggingReceive {
    case Authenticate(email, password) => {
      future {
        UserDAO.findOne(MongoDBObject("_id" -> email, "password" -> password))
      }.pipeTo(sender)
    }

    case Subscribe(user, feedLink, tagOpt, titleOpt) => {
      (aggregatorService ? AggregatorService.AddFeed(feedLink)).map {
        case r @ AggregatorService.AddFeedResult(feed) => {
          log.debug(r.toString)
          user.subscriptions.find { _.feedLink == feedLink } match {
            case Some(s) => tagOpt match {
              case Some(tag) => {
                if (!s.tags.contains(tag)) {
                  UserDAO.save(user.removeSubscription(s).addSubscription(s.addTag(tag)))
                  Success(true)
                }
                else {
                  Success(true)
                }
              }
              case None => Success(true)
            }
            case None => {
              UserDAO.save(user.addSubscription(Subscription(feedLink, titleOpt.getOrElse(feed.title), tagOpt.toSet)))
              Success(true)
            }
          }
        }
      }.recover {
        case t: Throwable => Failure(t)
      }.pipeTo(sender)
    }

    case Unsubscribe(user, feedLink) => {
      future {
        user.subscriptions.find { _.feedLink == feedLink } match {
          case Some(sub) => {
            UserDAO.save(user.removeSubscription(sub))
            Success(true)
          }
          case None => {
            Success(true)
          }
        }
      }.recover {
        case t: Throwable => Failure(t)
      }.pipeTo(sender)
    }

    case EditSubscription(user, feedLink, removeTag, addTag, newTitle) => {
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
            Success(true)
          }
          case None => {
            Success(true)
          }
        }
      }.recover {
        case t: Throwable => Failure(t)
      }.pipeTo(sender)
    }

    case ImportOpml(user, opml) => {
      val feedInfos = parseOpml(opml)
      log.debug("Parsed OPML: " + feedInfos)
      val addFeeds = feedInfos.map { feedInfo =>
        (aggregatorService ? AggregatorService.AddFeed(feedInfo.link)).mapTo[AggregatorService.AddFeedResult].map {
          case AggregatorService.AddFeedResult(feed) => {
            UserDAO.save(user.addSubscription(Subscription(feed.link, feedInfo.title, Set(feedInfo.folder))))
            feedInfo.link -> Success(true)
          }
        }.recover {
          case t: Throwable => feedInfo.link -> Failure(t)
        }
      }
      Future.sequence(addFeeds).map(_.toMap).pipeTo(sender)
    }

    case FetchArticles(user, feedLink, num, excludeTag, continuation) => future {
      user.subscriptions.find { _.feedLink == feedLink } match {
        case Some(sub) => {
          ArticleDAO.findByFeedLink(feedLink).sort(MongoDBObject("publishedDate" -> -1)).limit(num.getOrElse(20)).toSeq
        }
        case None => {
          Seq.empty[Article]
        }
      }
    }.pipeTo(sender)
  }

  private def parseOpml(opml: String): Seq[ImportedFeed] = {
    val root = XML.load(new StringReader(opml))
    val folders = root \ "body" \ "outline"
    folders.flatMap { elem =>
      val folder = (elem \ "@title").text
      val feeds = elem \ "outline"
      feeds map { elem =>
        val link = (elem \ "@xmlUrl").text
        val siteLink = (elem \ "@htmlUrl").text
        val title = (elem \ "@title").text
        ImportedFeed(link, siteLink, title, folder)
      }
    }
  }

  private case class ImportedFeed(link: String, siteLink: String, title: String, folder: String)
}

object UserService {
  case class Authenticate(email: String, password: String)
  case class Subscribe(user: User, feedLink: String, tag: Option[String], title: Option[String])
  case class Unsubscribe(user: User, feedLink: String)
  case class EditSubscription(user: User, feedLink: String, removeTag: Option[String], addTag: Option[String], newTitle: Option[String])
  case class ImportOpml(user: User, opml: String)
  case class FetchArticles(user: User, feedLink: String, num: Option[Int], excludeTag: Option[String], continuation: Option[String])
}
