package domain.services


import api.dto.{CreateShipmentDto, ShipmentDto}
import domain.models.ShipmentStatus

import java.util.UUID
import scala.concurrent.Future

trait ShipmentService {

  def createShipment(dto: CreateShipmentDto): Future[ShipmentDto]

  def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[ShipmentDto]]

  def updateShipmentStatus(
                            trackingNumber: String,
                            status: ShipmentStatus,
                            location: Option[String]
                          ): Future[ShipmentDto]

  def listShipments(): Future[Seq[ShipmentDto]]

  def deleteShipment(id: UUID): Future[Int]

}

