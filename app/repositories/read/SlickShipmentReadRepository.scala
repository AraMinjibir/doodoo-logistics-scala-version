package repositories.read

import scala.concurrent.{ExecutionContext, Future}
import infrastructure.persistence.tables.ShipmentsTable
import slick.jdbc.PostgresProfile.api._
import mappers.{ShipmentMapper, ShipmentRowMapper}
import domain.models.{Shipment, ShipmentStatus}
import infrastructure.persistence.tables.ShipmentsTable._
import slick.jdbc.JdbcProfile

import java.util.UUID


class SlickShipmentReadRepository(profile: JdbcProfile,
                                  db: JdbcProfile#Backend#Database,
                                  mapper: ShipmentRowMapper)(implicit ec: ExecutionContext)
  extends ShipmentReadRepository {

  private val q = ShipmentsTable.table

  override def getById(id: UUID): Future[Option[Shipment]] = {
    db.run(q.filter(_.id === id).result.headOption)
      .map(_.map(ShipmentMapper.fromRow))
  }

  override def getByStatus(status: ShipmentStatus): Future[Seq[Shipment]] = {
    db.run(q.filter(_.status === status).result)
      .map(_.map(ShipmentMapper.fromRow))
  }

  override def findByTrackingNumber(trackingNumber: String): Future[Option[Shipment]] = {
    db.run(q.filter(_.trackingNumber === trackingNumber).result.headOption)
      .map(_.map(ShipmentMapper.fromRow))
  }

  override def listAll(): Future[Seq[Shipment]] = {
    db.run(q.result)
      .map(_.map(ShipmentMapper.fromRow))
  }


}

