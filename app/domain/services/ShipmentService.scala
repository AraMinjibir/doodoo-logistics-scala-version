package domain.services


import api.dto.{CreateShipmentDto, ShipmentResponseDto}
import domain.models.ShipmentStatus

import java.util.UUID
import scala.concurrent.Future

trait ShipmentService {

  def createShipment(dto: CreateShipmentDto): Future[ShipmentResponseDto]

  def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[ShipmentResponseDto]]

  def updateShipmentStatus(
                            trackingNumber: String,
                            status: ShipmentStatus,
                            location: Option[String]
                          ): Future[ShipmentResponseDto]

  def listShipments(): Future[Seq[ShipmentResponseDto]]

  def deleteShipment(id: UUID): Future[Int]

}

