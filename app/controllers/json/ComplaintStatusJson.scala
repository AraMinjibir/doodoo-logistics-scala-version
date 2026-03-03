package controllers.json

import domain.models.ComplaintStatus
import domain.models.ComplaintStatus.fromString
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

object ComplaintStatusJson {

  implicit val format: Format[ComplaintStatus] = new Format[ComplaintStatus] {

    override def writes(status: ComplaintStatus): JsValue =
      JsString(ComplaintStatus.toString(status))

    override def reads(json: JsValue): JsResult[ComplaintStatus] =
      json match {
        case JsString(value) =>
          fromString(value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Invalid shipment status: $value"))

        case _ => JsError("ComplaintStatus must be a string")
      }
  }
}
