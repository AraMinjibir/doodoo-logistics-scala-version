package repositories

import com.google.inject.{Inject, Singleton}
import domain.models.{ProofOfDelivery, Shipment, ShipmentStatus, UserRole}
import infrastructure.persistence.tables.{ShipmentsTable, UserTable}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import infrastructure.persistence.tables.ShipmentsTable._
import infrastructure.persistence.tables.UserTable._

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

  override def uploadProofOfDelivery(
                                      shipmentId: UUID,
                                      proof: ProofOfDelivery
                                    ): Future[Option[Shipment]] = {

    val action =
      q.filter(_.id === shipmentId)
        .result
        .head
        .flatMap { existingRow =>

          val updatedProofs = existingRow.proofOfDelivery :+ proof

          q.filter(_.id === shipmentId)
            .map(_.proofOfDelivery)
            .update(updatedProofs)
            .flatMap { _ =>
              q.filter(_.id === shipmentId).result.head.map(row => Some(row))
            }
        }

    db.run(action.transactionally)
      .map(_.map(mapper.fromRow))
  }

 override def assignServiceProvider(
                             shipmentId: UUID,
                             providerId: UUID
                           ): Future[Try[Int]] = {

    val spRole: UserRole = UserRole.ServiceProvider

    val action = for {
      // 1. Check if shipment exists
      shipmentOpt <- ShipmentsTable.table.filter(_.id === shipmentId).result.headOption
      shipment <- shipmentOpt match {
        case Some(s) => DBIO.successful(s)
        case None    => DBIO.failed(new IllegalArgumentException("No shipment found"))
      }

      // 2. Check if user exists and is a service provider
      providerExists <- UserTable.table
        .filter(u => u.id === providerId && u.role === spRole)
        .exists
        .result
      _ <- if (providerExists)
        DBIO.successful(())
      else
        DBIO.failed(new IllegalArgumentException("User is not a service provider"))

      // 3. Update shipment with providerId and status
      updatedRows <- ShipmentsTable.table
        .filter(_.id === shipmentId)
        .map(s => (s.serviceProviderId, s.status))
        .update((Some(providerId), ShipmentStatus.Assigned))
    } yield updatedRows

    db.run(action.transactionally.asTry)
  }




}
