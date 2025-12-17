package domain.services.impl

import api.dto.{CreateShipmentDto, ShipmentResponseDto}
import domain.models.{Dimensions, PackageDetails, Recipient, Shipment, ShipmentStatus, TrackingEvent}
import domain.services.ShipmentService
import domain.validation.ShipmentValidation
import repositories.read.ShipmentReadRepository
import repositories.write.ShipmentWriteRepository
import mappers.ShipmentMapper
import utilities.{CostCalculator, DateEstimator, TrackingNumberGenerator}

import java.time.Instant
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShipmentServiceImpl @Inject()(
                                     writeRepo: ShipmentWriteRepository,
                                     readRepo: ShipmentReadRepository,
                                     validation: ShipmentValidation,
                                     costCalculator: CostCalculator,
                                     dateEstimator: DateEstimator
                                   )(implicit ec: ExecutionContext) extends ShipmentService {


  // CREATE

  override def createShipment(dto: CreateShipmentDto): Future[Shipment] = {
    validation.validateCreate(dto) match {
      case Left(err) => Future.failed(new IllegalArgumentException(err))
      case Right(_) =>
        val now = Instant.now()
        val base = ShipmentMapper.toDomain(dto)
        val tracking = TrackingNumberGenerator.generate()

        val initialEvent = TrackingEvent(
          status = ShipmentStatus.Created,
          timestamp = now,
          location = None
        )

        val shipmentToSave =
          base.copy(
            trackingNumber = Some(tracking),
            createdAt = now,
            history = Seq(initialEvent),
            cost = costCalculator.calculate(base.packageDetails, base.recipient),
            estimatedDeliveryDate = dateEstimator.estimate(base.recipient)
          )

        for {
          _ <- writeRepo.create(shipmentToSave)
        } yield shipmentToSave
    }
  }

  // GET BY TRACKING NUMBER

  override def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[Shipment]] = {
    readRepo
      .findByTrackingNumber(trackingNumber)
  }

  override def getShipmentById(id: UUID): Future[Option[Shipment]] = {
    readRepo.getById(id)
  }

  override def getShipmentByStatus(shipmentStatus: ShipmentStatus): Future[Seq[Shipment]] = {
    readRepo.getByStatus(shipmentStatus)
  }

  // UPDATE STATUS
  override def updateShipmentStatus(
                                     trackingNumber: String,
                                     status: ShipmentStatus,
                                     location: Option[String]
                                   ): Future[Either[String, Shipment]] = {

    val now = Instant.now()

    readRepo.findByTrackingNumber(trackingNumber).flatMap {
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

            writeRepo.update(updated).map(_ => Right(updated))
        }
    }
  }

  override def updateShipment(id: UUID, dto: CreateShipmentDto): Future[Either[String, Shipment]] = {
    val now = Instant.now()

    readRepo.getById(id).flatMap {
      case None =>
        Future.successful(Left(s"Shipment with ID $id not found"))

      case Some(existing) =>
        // 1. Create the updated Domain Model from the DTO
        val updated = existing.copy(
          senderName = dto.senderName,
          recipient = Recipient(
            name = dto.recipient.name,
            address = dto.recipient.address,
            contact = dto.recipient.contact
          ),
          packageDetails = PackageDetails(
            weight = dto.packageDetails.weight,
            dimensions = Dimensions(
              length = dto.packageDetails.dimensions.length,
              width = dto.packageDetails.dimensions.width,
              height = dto.packageDetails.dimensions.height
            ),
            contents = dto.packageDetails.contents
          ),
          updatedAt = now
        )

        // 2. Persist and return the Domain Model
        writeRepo.update(updated).map(_ => Right(updated))
    }.recover {
      case e: Exception => Left(s"Database error: ${e.getMessage}")
    }
  }

  // LIST ALL
  override def listShipments(): Future[Seq[Shipment]] = {
    readRepo.listAll()
  }

    // DELETE

  override def deleteShipment(id: UUID): Future[Either[String, Unit]] = {
    writeRepo.delete(id).map { rowsAffected =>
      if (rowsAffected > 0)
        Right(())
      else
        Left(s"Shipment with ID $id not found or already deleted")
    }
  }}
