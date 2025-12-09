package repositories.write

import domain.models.Shipment

import java.util.UUID
import scala.concurrent.Future

trait ShipmentWriteRepository {
  def create(shipment: Shipment): Future[Int]
  def update(shipment: Shipment): Future[Int]
  def delete(id: UUID): Future[Int]
}
