package domain.models

import domain.errors.{DomainError, InvalidShipmentStatusTransition}
import play.api.mvc.PathBindable

sealed trait ShipmentStatus extends Product with Serializable

object ShipmentStatus {

  case object Created extends ShipmentStatus
  case object Assigned extends ShipmentStatus
  case object InTransit extends ShipmentStatus
  case object OutForDelivery extends ShipmentStatus
  case object Delivered extends ShipmentStatus
  case object Cancelled extends ShipmentStatus

  val values: Seq[ShipmentStatus] =
    Seq(Created,Assigned, InTransit, OutForDelivery, Delivered, Cancelled)
  private val allowedTransitions: Map[ShipmentStatus, Set[ShipmentStatus]] = Map(
    ShipmentStatus.Created        -> Set(ShipmentStatus.Assigned, ShipmentStatus.InTransit, ShipmentStatus.Cancelled),
    ShipmentStatus.Assigned      -> Set(ShipmentStatus.InTransit,ShipmentStatus.OutForDelivery, ShipmentStatus.Cancelled),
    ShipmentStatus.InTransit -> Set(ShipmentStatus.OutForDelivery,ShipmentStatus.Delivered, ShipmentStatus.Cancelled),
    ShipmentStatus.OutForDelivery      -> Set(ShipmentStatus.Delivered,ShipmentStatus.Cancelled),
    ShipmentStatus.Delivered      -> Set.empty,
    ShipmentStatus.Cancelled      -> Set.empty,

  )


  def fromString(value: String): Option[ShipmentStatus] =
    values.find(toString(_) == value)

  def toString(status: ShipmentStatus): String =
    status match {
      case Created           => "Created"
      case Assigned          =>  "Assigned"
      case InTransit         => "InTransit"
      case OutForDelivery    => "OutForDelivery"
      case Delivered         => "Delivered"
      case Cancelled         => "Cancelled"
    }

  //  Path bindable for type-safe routing
  implicit def pathBindable(implicit stringBinder: PathBindable[String]): PathBindable[ShipmentStatus] =
    new PathBindable[ShipmentStatus] {
      override def bind(key: String, value: String): Either[String, ShipmentStatus] = {
        fromString(value).toRight(s"Status '$value' is not a valid ShipmentStatus")
      }

      override def unbind(key: String, value: ShipmentStatus): String = value.toString
    }

  def validateTransition(current: ShipmentStatus, next: ShipmentStatus): Either[DomainError, Unit] = {
    val allowedNext = allowedTransitions.getOrElse(current, Set.empty)

    if (!allowedNext.contains(next))
      Left(InvalidShipmentStatusTransition(current, next))
    else
      Right(())
  }


}
