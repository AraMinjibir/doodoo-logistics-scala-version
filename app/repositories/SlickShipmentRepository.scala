package repositories

import com.google.inject.{Inject, Singleton}
import domain.models.{Shipment, ShipmentStatus}
import infrastructure.persistence.tables.ShipmentsTable
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import infrastructure.persistence.tables.ShipmentsTable._
import mappers.ShipmentRowMapper
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SlickShipmentRepository @Inject()(dbConfigProvider: DatabaseConfigProvider,
                                        mapper:ShipmentRowMapper)
                                       (implicit ec: ExecutionContext)
  extends ShipmentRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig.profile.api._
  private val db = dbConfig.db

  private val q = ShipmentsTable.table

  override def create(shipment: Shipment): Future[Try[Int]] = {
    val row = mapper.toRow(shipment)
    val sql = q += row
    db.run(sql.asTry)
  }

  override def update(shipment: Shipment): Future[Try[Int]] = {
    val row = mapper.toRow(shipment)
    val query = q.filter(_.id === shipment.id).update(row)
    db.run(query.asTry)
  }

  override def delete(id: UUID):  Future[Try[Int]] = {
    val count = q.filter(_.id === id).delete
    db.run(count.asTry)
  }

  override def getById(id: UUID): Future[Option[Shipment]] = {
    db.run(q.filter(_.id === id).result.headOption)
      .map(_.map(mapper.fromRow))
  }

  override def getByStatus(status: ShipmentStatus): Future[Seq[Shipment]] = {
    db.run(q.filter(_.status === status).result)
      .map(_.map(mapper.fromRow))
  }

  override def findByTrackingNumber(trackingNumber: String): Future[Option[Shipment]] = {
    db.run(q.filter(_.trackingNumber === trackingNumber).result.headOption)
      .map(_.map(mapper.fromRow))
  }

  override def listAll(offset: Int, limit: Int): Future[Seq[Shipment]] = {
    db.run(
      q.sortBy(_.createdAt.desc) // 1. Always sort by newest first
        .drop(offset)             // 2. Skip previous pages
        .take(limit)              // 3. Only fetch one page's worth
        .result
    ).map(_.map(mapper.fromRow))
  }

}
