package domain.services


import api.dto.{CreateShipmentDto, ShipmentResponseDto}
import domain.models.{Shipment, ShipmentStatus}

import java.util.UUID
import scala.concurrent.Future

trait ShipmentService {

  def createShipment(dto: CreateShipmentDto): Future[Shipment]

  def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[Shipment]]
  def getShipmentById(id:UUID):Future[Option[Shipment]]
  def getShipmentByStatus(shipmentStatus: ShipmentStatus):Future[Seq[Shipment]]

  def updateShipmentStatus(
                            trackingNumber: String,
                            status: ShipmentStatus,
                            location: Option[String]
                          ): Future[Either[String, Shipment]]

  def updateShipment(shipmentId:UUID, shipment: CreateShipmentDto): Future[Either[String, Shipment]]

  def listShipments(): Future[Seq[Shipment]]

  def deleteShipment(id: UUID): Future[Either[String, Unit]]

}

