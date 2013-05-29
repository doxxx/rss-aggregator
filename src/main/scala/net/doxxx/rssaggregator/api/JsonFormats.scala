package net.doxxx.rssaggregator.api

import spray.json._
import spray.json.DefaultJsonProtocol._
import java.util.Date

object JsonFormats {

  case class GRSubscription(id: String, title: String, categories: Seq[Map[String,String]], sortid: String, firstitemmsec: String)

  implicit val subscriptionsFormat = jsonFormat5(GRSubscription)

  implicit object jsonDateFormat extends RootJsonFormat[Date] {
    def write(obj: Date): JsValue = JsNumber(obj.getTime)

    def read(json: JsValue) = json match {
      case JsNumber(t) => new Date(t.toLongExact)
      case _ => throw new IllegalArgumentException("Date expected")
    }
  }

}
