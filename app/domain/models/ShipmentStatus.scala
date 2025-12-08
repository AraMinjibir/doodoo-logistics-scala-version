package domain.models

sealed trait ShipmentStatus

object ShipmentStatus {
  case object Draft extends ShipmentStatus
  case object Pending extends ShipmentStatus
  case object InTransit extends ShipmentStatus
  case object OutForDelivery extends ShipmentStatus
  case object Delivered extends ShipmentStatus
  case object Cancelled extends ShipmentStatus

  def fromString(value: String): ShipmentStatus =
    value match {
      case "Draft"            => Draft
      case "Pending"          => Pending
      case "InTransit"        => InTransit
      case "OutForDelivery"   => OutForDelivery
      case "Delivered"        => Delivered
      case "Cancelled"        => Cancelled
      case other              => throw new IllegalArgumentException(s"Invalid shipment status: $other")
    }

  def toString(status: ShipmentStatus): String =
    status match {
      case Draft           => "Draft"
      case Pending         => "Pending"
      case InTransit       => "InTransit"
      case OutForDelivery  => "OutForDelivery"
      case Delivered       => "Delivered"
      case Cancelled       => "Cancelled"
    }
}
