package domain.models

import play.api.libs.json._
import play.api.mvc.PathBindable

sealed trait ShipmentStatus extends Product with Serializable

object ShipmentStatus {

  case object Pending extends ShipmentStatus
  case object Created extends ShipmentStatus
  case object InTransit extends ShipmentStatus
  case object OutForDelivery extends ShipmentStatus
  case object Delivered extends ShipmentStatus
  case object Cancelled extends ShipmentStatus

  val values: Seq[ShipmentStatus] =
    Seq(Pending, Created, InTransit, OutForDelivery, Delivered, Cancelled)

  def fromString(value: String): Option[ShipmentStatus] =
    values.find(toString(_) == value)

  def toString(status: ShipmentStatus): String =
    status match {
      case Pending           => "Pending"
      case Created           => "Created"
      case InTransit         => "InTransit"
      case OutForDelivery    => "OutForDelivery"
      case Delivered         => "Delivered"
      case Cancelled         => "Cancelled"
    }

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

//  Path bindable for type-safe routing
  implicit def pathBindable(implicit stringBinder: PathBindable[String]): PathBindable[ShipmentStatus] =
    new PathBindable[ShipmentStatus] {
      override def bind(key: String, value: String): Either[String, ShipmentStatus] = {
        fromString(value).toRight(s"Status '$value' is not a valid ShipmentStatus")
      }

      override def unbind(key: String, value: ShipmentStatus): String = value.toString
    }
}
