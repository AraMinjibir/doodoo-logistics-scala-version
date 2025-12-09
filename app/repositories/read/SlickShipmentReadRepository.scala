package repositories.read

import scala.concurrent.{ExecutionContext, Future}
import infrastructure.persistence.tables.ShipmentsTable
import slick.jdbc.PostgresProfile.api._
import mappers.ShipmentMapper
import domain.models.Shipment
import java.util.UUID

class SlickShipmentReadRepository(db: Database)(implicit ec: ExecutionContext)
  extends ShipmentReadRepository {

  private val q = ShipmentsTable.table

  override def getById(id: UUID): Future[Option[Shipment]] = {
    db.run(q.filter(_.id === id).result.headOption).map(_.map(ShipmentMapper.toDomain))
  }
  override def findByTrackingNumber(trackingNumber: String): Future[Option[Shipment]] = {
    db.run(q.filter(_.trackingNumber === trackingNumber).result.headOption).map(_.map(ShipmentMapper.toDomain))
  }
  override def listAll(): Future[Seq[Shipment]] = {
    db.run(q.result).map(_.map(ShipmentMapper.toDomain))
  }
}
