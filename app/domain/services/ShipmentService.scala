package domain.services


import api.dto.{CreateShipmentDto, ShipmentResponseDto}
import domain.models.{Shipment, ShipmentStatus}

import java.util.UUID
import scala.concurrent.Future

trait ShipmentService {

  def createShipment(dto: CreateShipmentDto): Future[Shipment]

  def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[ShipmentResponseDto]]
  def getShipmentById(id:UUID):Future[Option[ShipmentResponseDto]]
  def getShipmentByStatus(shipmentStatus: ShipmentStatus):Future[Seq[ShipmentResponseDto]]

  def updateShipmentStatus(
                            trackingNumber: String,
                            status: ShipmentStatus,
                            location: Option[String]
                          ): Future[ShipmentResponseDto]

  def listShipments(): Future[Seq[ShipmentResponseDto]]

  def deleteShipment(id: UUID): Future[Int]

}

