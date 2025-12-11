package domain.validation

import api.dto.CreateShipmentDto
import domain.models.ShipmentStatus

trait ShipmentValidation {
  def validateCreate(dto: CreateShipmentDto): Either[String, Unit]

  def validateTransition(
                          current: ShipmentStatus,
                          next: ShipmentStatus
                        ): Either[String, Unit]
}
