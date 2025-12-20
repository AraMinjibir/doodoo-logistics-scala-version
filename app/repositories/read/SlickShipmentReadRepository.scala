package repositories.read

import com.google.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import infrastructure.persistence.tables.ShipmentsTable
import mappers.{ShipmentMapper, ShipmentRowMapper}
import domain.models.{Shipment, ShipmentStatus}
import infrastructure.persistence.tables.ShipmentsTable._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.util.UUID


@Singleton
class SlickShipmentReadRepository@Inject()(dbConfigProvider: DatabaseConfigProvider,
                                           mapper: ShipmentRowMapper)
                                          (implicit ec: ExecutionContext)
  extends ShipmentReadRepository {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig.profile.api._
  private val db = dbConfig.db
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

 override def listAll(offset: Int, limit: Int): Future[Seq[Shipment]] = {
    db.run(
      q.sortBy(_.createdAt.desc) // 1. Always sort by newest first
        .drop(offset)             // 2. Skip previous pages
        .take(limit)              // 3. Only fetch one page's worth
        .result
    ).map(_.map(ShipmentMapper.fromRow))
  }


}

