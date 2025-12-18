package repositories.write

import com.google.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.JdbcProfile
import infrastructure.persistence.tables.ShipmentsTable
import mappers.ShipmentRowMapper
import domain.models.Shipment
import play.api.db.slick.DatabaseConfigProvider

import java.util.UUID

@Singleton
class SlickShipmentWriteRepository @Inject()(dbConfigProvider: DatabaseConfigProvider,
                                            mapper: ShipmentRowMapper)
                                           (implicit ec: ExecutionContext)
  extends ShipmentWriteRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig.profile.api._
  private val db = dbConfig.db

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
