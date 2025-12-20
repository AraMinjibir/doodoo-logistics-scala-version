package repositories.read

import domain.models.{Shipment, ShipmentStatus}

import scala.concurrent.Future
import java.util.UUID

trait ShipmentReadRepository {
  def getById(id: UUID): Future[Option[Shipment]]
  def getByStatus(status:ShipmentStatus):Future[Seq[Shipment]]
  def listAll(offset: Int, limit: Int): Future[Seq[Shipment]]
  def findByTrackingNumber(trackingNumber: String) : Future[Option[Shipment]]
}
