package domain.services

import domain.errors.DomainError
import domain.models.{Shipment, ShipmentStatus}

import java.util.UUID
import scala.concurrent.Future

trait ShipmentService {

  def createShipment(shipment: Shipment): Future[Either[DomainError, Shipment]]

  def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[Shipment]]
  def getShipmentById(id:UUID):Future[Option[Shipment]]
  def getShipmentByStatus(shipmentStatus: ShipmentStatus):Future[Seq[Shipment]]

  def updateShipmentStatus(
                            trackingNumber: String,
                            status: ShipmentStatus,
                            location: Option[String]
                          ): Future[Either[DomainError, Shipment]]

  def updateShipment(shipmentId:UUID, shipment: Shipment): Future[Either[DomainError, Shipment]]

  def listShipments(offset: Int, limit: Int): Future[Seq[Shipment]]
  def deleteShipment(id: UUID): Future[Either[String, Unit]]

}

