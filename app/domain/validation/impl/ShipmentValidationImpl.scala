package domain.validation.impl

import domain.models.{Shipment, ShipmentStatus}
import domain.validation.ShipmentValidation

import javax.inject.Singleton

@Singleton
class ShipmentValidationImpl extends ShipmentValidation {

  private val allowedTransitions: Map[ShipmentStatus, Set[ShipmentStatus]] = Map(
    ShipmentStatus.Created        -> Set(ShipmentStatus.InTransit, ShipmentStatus.Cancelled),
    ShipmentStatus.InTransit      -> Set(ShipmentStatus.OutForDelivery, ShipmentStatus.Cancelled),
    ShipmentStatus.OutForDelivery -> Set(ShipmentStatus.Delivered, ShipmentStatus.Cancelled),
    ShipmentStatus.Delivered      -> Set.empty,
    ShipmentStatus.Cancelled      -> Set.empty
  )

  override def validateCreate(shipment:Shipment): Either[String, Unit] = {

    // Validate sender
    if (shipment.senderName.trim.isEmpty)
      return Left("Sender name is required")

    // Validate recipient fields
    if (shipment.recipient.name.trim.isEmpty)
      return Left("Recipient name is required")

    if (shipment.recipient.address.toString.trim.isEmpty)
      return Left("Recipient address is required")

    if (shipment.recipient.contact.trim.isEmpty)
      return Left("Recipient contact is required")

    // Validate package details
    if (shipment.packageDetails.contents.trim.length < 3)
      return Left("Package contents description must be at least 3 characters")

    if (shipment.packageDetails.weight <= 0)
      return Left("Package weight must be greater than zero")

    val dims = shipment.packageDetails.dimensions

    if (dims.length <= 0 || dims.width <= 0 || dims.height <= 0)
      return Left("Package dimensions must all be greater than zero")

    Right(())
  }

  override def validateTransition(current: ShipmentStatus, next: ShipmentStatus): Either[String, Unit] = {
    val allowedNext = allowedTransitions.getOrElse(current, Set.empty)

    if (!allowedNext.contains(next))
      Left(s"Invalid shipment status transition from ${ShipmentStatus.toString(current)} to ${ShipmentStatus.toString(next)}")
    else
      Right(())
  }
}
