package repositories.write

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcProfile
import infrastructure.persistence.tables.ShipmentsTable
import mappers.ShipmentRowMapper
import domain.models.Shipment

import java.util.UUID

class SlickShipmentWriteRepository(
                                    profile: JdbcProfile,
                                    db: JdbcProfile#Backend#Database,
                                    mapper: ShipmentRowMapper
                                  )(implicit ec: ExecutionContext)
  extends ShipmentWriteRepository {

  import profile.api._

  private val q = ShipmentsTable.table

  override def create(shipment: Shipment): Future[Int] = {
    val row = mapper.toRow(shipment)
    db.run(q += row)
  }

  override def update(shipment: Shipment): Future[Int] = {
    val row = mapper.toRow(shipment)
    db.run(q.filter(_.id === shipment.id).update(row))
  }

  override def delete(id: UUID): Future[Int] = {
    db.run(q.filter(_.id === id).delete)
  }
}
