package domain.services.impl
import controllers.helpers.ResultMapper
import domain.errors.DomainError
import domain.models.{Dimensions, PackageDetails, Recipient, Shipment, ShipmentStatus, TrackingEvent}
import domain.services.ShipmentService
import repositories.ShipmentRepository

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ShipmentServiceImpl @Inject()(
                                     repo: ShipmentRepository,
                                   )(implicit ec: ExecutionContext) extends ShipmentService with ResultMapper {


  // CREATE

  override def createShipment(shipment: Shipment): Future[Either[DomainError,Shipment]] = {
    repo
      .create(shipment)
      .map{
          case Success(_)  => Right(shipment)
          case Failure(ex) => Left(mapInsertException(ex))
        }
  }


  // GET BY TRACKING NUMBER

  override def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[Shipment]] = {
    repo.findByTrackingNumber(trackingNumber)
  }

  override def getShipmentById(id: UUID): Future[Option[Shipment]] = {
    repo.getById(id)
  }

  override def getShipmentByStatus(shipmentStatus: ShipmentStatus): Future[Seq[Shipment]] = {
    repo.getByStatus(shipmentStatus)
  }

  // UPDATE STATUS
  override def updateShipmentStatus(
                                     trackingNumber: String,
                                     status: ShipmentStatus,
                                     location: Option[String]
                                   ): Future[Either[String, Shipment]] = {

    val now = Instant.now()

    repo.findByTrackingNumber(trackingNumber).flatMap {
      case None =>
        Future.successful(Left(s"Shipment $trackingNumber not found"))

      case Some(shipment) =>
        validation.validateTransition(shipment.status, status) match {
          case Left(err) =>
            Future.successful(Left(err))

          case Right(_) =>
            val updated = shipment.copy(
              status = status,
              updatedAt = now,
              history = shipment.history :+ TrackingEvent(status, now, location)
            )

            repo.update(updated).map{
              case  Success(_) => Right(updated)
              case Failure(ex) => Left(ex.getMessage)
            }
        }
    }
  }

  override def updateShipment(id: UUID, shipment: Shipment): Future[Either[String, Shipment]] = {
    val now = Instant.now()

    repo.getById(id).flatMap {
      case None =>
        Future.successful(Left(s"Shipment with ID $id not found"))

      case Some(existing) =>
        // 1. Create the updated Domain Model
        val updated = existing.copy(
          senderName = shipment.senderName,
          recipient = Recipient(
            name = shipment.recipient.name,
            address = shipment.recipient.address,
            contact = shipment.recipient.contact
          ),
          packageDetails = PackageDetails(
            weightInKilograms = shipment.packageDetails.weightInKilograms,
            dimensions = Dimensions(
              lengthInCentimeters = shipment.packageDetails.dimensions.lengthInCentimeters,
              widthInCentimeters = shipment.packageDetails.dimensions.widthInCentimeters,
              heightInCentimeters = shipment.packageDetails.dimensions.heightInCentimeters
            ),
            contents = shipment.packageDetails.contents
          ),
          updatedAt = now
        )

        // 2. Persist and return the Domain Model
        repo.update(updated).map{
          case Success(_) => Right(updated)
          case Failure(ex) => Left(ex.getMessage)
        }
    }
  }

  // LIST ALL
  override def listShipments(offset: Int, limit: Int): Future[Seq[Shipment]] = {
    repo.listAll(offset: Int, limit: Int)
  }

    // DELETE

  override def deleteShipment(id: UUID): Future[Either[String, Unit]] = {
    repo.delete(id).map {
      case Success(0) =>
        Left("Shipment not found")

      case Success(_) =>
        Right(())

      case Failure(ex) =>
        Left(ex.getMessage)
    }
  }

}
