package net.doxxx.rssaggregator.api

import spray.json.{JsNumber, JsValue, RootJsonFormat, DefaultJsonProtocol}
import net.doxxx.rssaggregator.model.{Article, Feed}
import java.util.Date

/**
 * Created 13-03-26 11:33 PM by gordon.
 */
object AggregatorJsonProtocol extends DefaultJsonProtocol {

  implicit val feedFormat = jsonFormat4(Feed.apply)
  implicit val articleFormat = jsonFormat8(Article.apply)
  implicit object dateFormat extends RootJsonFormat[Date] {
    def write(obj: Date): JsValue = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(t) => new Date(t.toLongExact)
      case _ => throw new IllegalArgumentException("Date expected")
    }
  }
}
