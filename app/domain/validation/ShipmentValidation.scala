package domain.validation

import domain.models.{Shipment, ShipmentStatus}

trait ShipmentValidation {
  def validateCreate(shipment: Shipment): Either[String, Unit]

  def validateTransition(
                          current: ShipmentStatus,
                          next: ShipmentStatus
                        ): Either[String, Unit]
}
