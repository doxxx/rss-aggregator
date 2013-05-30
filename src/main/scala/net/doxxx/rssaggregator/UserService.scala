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

/**
 * Created 13-05-13 8:18 AM by gordon.
 */
class UserService extends Actor with ActorLogging {
  import UserService._

  private implicit val executionContext = context.dispatcher
  private implicit val timeout = Timeout(10.seconds)

  private var aggregatorService: ActorRef = _

  override def preStart() {
    aggregatorService = context.actorFor(context.system.settings.config.getString("aggregator-service-path"))
  }

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
  }
}

object UserService {
  case class Authenticate(email: String, password: String)
  case class Subscribe(user: User, feedLink: String, tag: Option[String], title: Option[String])
  case class Unsubscribe(user: User, feedLink: String)
  case class EditSubscription(user: User, feedLink: String, removeTag: Option[String], addTag: Option[String], newTitle: Option[String])
}
