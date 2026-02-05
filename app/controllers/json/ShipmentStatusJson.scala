package controllers.json

import domain.models.ShipmentStatus
import domain.models.ShipmentStatus.fromString
import play.api.libs.json._
import play.api.mvc.PathBindable

object ShipmentStatusJson {

  implicit val format: Format[ShipmentStatus] = new Format[ShipmentStatus] {

    override def writes(status: ShipmentStatus): JsValue =
      JsString(ShipmentStatus.toString(status))

    override def reads(json: JsValue): JsResult[ShipmentStatus] =
      json match {
        case JsString(value) =>
          fromString(value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Invalid shipment status: $value"))

        case _ => JsError("ShipmentStatus must be a string")
      }
  }
}
