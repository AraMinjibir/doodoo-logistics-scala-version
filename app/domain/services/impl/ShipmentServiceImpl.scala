package domain.services.impl
import controllers.helpers.ResultMapper
import domain.errors.{DomainError, NotAServiceProvide, ShipmentNotFound, ShipmentNotFoundById, UpdateProofOfDeliveryError, UpdateShipmentStatusError, UserNotFound, UserNotFoundWithId, ValidationError}
import domain.models.{ProofOfDelivery, Shipment, ShipmentStatus, UserRole}
import domain.services.ShipmentService
import repositories.{ShipmentRepository, UserRepository}

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ShipmentServiceImpl @Inject()(
                                     repo: ShipmentRepository,
                                     userRepository: UserRepository
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
  override def deleteShipment(id: UUID): Future[Either[DomainError, Unit]] = {
    repo.delete(id).map{
      case Success(_) => Right(())
      case Success(0) => Left(ShipmentNotFoundById(id))
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

  override def assignServiceProviderToShipment(
                                                shipmentId: UUID,
                                                providerId: UUID
                                              ): Future[Either[DomainError, Shipment]] = {
    // 1. Check that shipment exists
    repo.getById(shipmentId).flatMap {
      case None =>
        Future.successful(Left(ShipmentNotFound(shipmentId.toString)))

      case Some(shipment) =>
        // 2. Check that the user exists and is a service provider
        userRepository.findUserById(providerId).flatMap {
          case None =>
            Future.successful(Left(UserNotFoundWithId(providerId)))

          case Some(user) if user.role != UserRole.ServiceProvider =>
            Future.successful(Left(NotAServiceProvide(providerId)))

          case Some(_) =>
            // 3. Assign provider and update status
            repo.assignServiceProvider(shipmentId, providerId).map {
              case scala.util.Success(_) =>
                // 4. Return the updated shipment
                val updatedShipment = shipment.copy(
                  serviceProviderId = Some(providerId),
                  status = ShipmentStatus.Assigned,
                  updatedAt = Instant.now()
                )
                Right(updatedShipment)

              case Failure(ex) =>
                Left(mapInsertException(ex))
            }
        }
    }
  }

  override def getShipmentsForProvider(providerId: UUID): Future[Seq[Shipment]] = {
    repo.findByServiceProvider(providerId)
  }
}
