package domain.services.impl
import controllers.helpers.ResultMapper
import domain.errors.{DomainError, ShipmentNotFound, ShipmentNotFoundById, UpdateProofOfDeliveryError, UpdateShipmentStatusError}
import domain.models.{ProofOfDelivery, Shipment, ShipmentStatus}
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
  override def updateShipmentStatus(
                                     trackingNumber: String,
                                     status: ShipmentStatus,
                                     location: Option[String]
                                   ): Future[Either[DomainError, Shipment]] = {
    repo.findByTrackingNumber(trackingNumber).flatMap {
      case None =>
        Future.successful(Left(ShipmentNotFound(trackingNumber)))

      case Some(shipment) =>
        shipment.updateStatus(status)
        .fold(
          err => Future.successful(Left(err)),
          updated => repo.update(updated).map{
            case Success(_) =>  Right(updated)
            case Failure(err) => Left(UpdateShipmentStatusError(err.getMessage))
          }
        )
    }
  }

  override def updateShipment(id: UUID, shipment: Shipment): Future[Either[DomainError, Shipment]] = {
    val now = Instant.now()

    repo.getById(id).flatMap {
      case None =>
        Future.successful(Left(ShipmentNotFoundById(id)))

      case Some(existing) =>
       val updated = existing.copy(
         senderName = shipment.senderName,
         recipient = shipment.recipient,
         packageDetails = shipment.packageDetails,
         updatedAt = now
       )
        repo.update(updated).map{
          case Success(_) => Right(updated)
          case Failure(ex) => Left(UpdateShipmentStatusError(ex.getMessage))
        }
    }
  }
  override def listShipments(offset: Int, limit: Int): Future[Seq[Shipment]] = {
    repo.listAll(offset: Int, limit: Int)
  }
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

  override def uploadProofOfDelivery(
                                      trackingNumber: String,
                                      proof: ProofOfDelivery
                                    ): Future[Either[List[DomainError], Shipment]] = {

    ProofOfDelivery
      .createProofOfDelivery(
        image = proof.image,
        note = proof.note,
        submittedBy = proof.submittedBy,
        submittedAt = proof.submittedAt
      ) match {

      case Left(errors) =>
        Future.successful(Left(errors))

      case Right(validProof) =>
        repo.findByTrackingNumber(trackingNumber).flatMap {

          case None =>
            Future.successful(Left(List[DomainError](ShipmentNotFound(trackingNumber))))

          case Some(shipment) =>
            shipment.attachProofOfDelivery(validProof) match {

              case Left(error) =>
                Future.successful(Left(List[DomainError](error)))

              case Right(_) =>
                repo.uploadProofOfDelivery(shipment.id, validProof).map {
                  case None =>
                    Left(List(UpdateProofOfDeliveryError("Persistence failure")))
                  case Some(saved) =>
                    Right(saved)
                }
            }
        }
    }
  }


}
