package repositories.write

import scala.concurrent.{ExecutionContext, Future}
import infrastructure.persistence.tables.ShipmentsTable
import slick.jdbc.PostgresProfile.api._
import mappers.ShipmentMapper
import domain.models.Shipment

import java.util.UUID


class SlickShipmentWriteRepository(db: Database)(implicit ec: ExecutionContext)
  extends ShipmentWriteRepository {

  private val q = ShipmentsTable.table

  override def create(shipment: Shipment): Future[Int] = {
    val row = ShipmentMapper.toRow(shipment)
    db.run(q += row)
  }

  override def update(shipment: Shipment): Future[Int] = {
    val row = ShipmentMapper.toRow(shipment)
    db.run(q.filter(_.id === shipment.id).update(row))
  }
  override def delete(id: UUID): Future[Int] = {
    db.run(q.filter(_.id === id).delete)
  }
}
