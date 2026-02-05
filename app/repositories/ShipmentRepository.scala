package repositories

import domain.models.{Shipment, ShipmentStatus}

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

trait ShipmentRepository {
  def create(shipment: Shipment): Future[Try[Int]]
  def update(shipment: Shipment):  Future[Try[Int]]
  def delete(id: UUID):  Future[Try[Int]]
  def getById(id: UUID): Future[Option[Shipment]]
  def getByStatus(status:ShipmentStatus):Future[Seq[Shipment]]
  def listAll(offset: Int, limit: Int): Future[Seq[Shipment]]
  def findByTrackingNumber(trackingNumber: String) : Future[Option[Shipment]]
}
