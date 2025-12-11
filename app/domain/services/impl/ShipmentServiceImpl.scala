package domain.services.impl

import api.dto.{CreateShipmentDto, ShipmentResponseDto}
import domain.models.{ShipmentStatus, TrackingEvent}
import domain.services.ShipmentService
import domain.validation.ShipmentValidation
import repositories.read.ShipmentReadRepository
import repositories.write.ShipmentWriteRepository
import mappers.ShipmentMapper
import utilities.TrackingNumberGenerator

import java.time.Instant
import java.time.Instant.now
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShipmentServiceImpl @Inject()(
                                     writeRepo: ShipmentWriteRepository,
                                     readRepo: ShipmentReadRepository,
                                     validation: ShipmentValidation
                                   )(implicit ec: ExecutionContext) extends ShipmentService {


  // CREATE

  override def createShipment(dto: CreateShipmentDto): Future[ShipmentResponseDto] = {
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
            history = Seq(initialEvent)
          )

        for {
          _ <- writeRepo.create(shipmentToSave)
          savedOpt <- readRepo.findByTrackingNumber(tracking)
        } yield ShipmentMapper.toDto(savedOpt.getOrElse(shipmentToSave))
    }
  }

  // GET BY TRACKING NUMBER

  override def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[ShipmentResponseDto]] = {
    readRepo
      .findByTrackingNumber(trackingNumber)
      .map(_.map(ShipmentMapper.toDto))
  }

  // UPDATE STATUS
  override def updateShipmentStatus(
                                     trackingNumber: String,
                                     status: ShipmentStatus,
                                     location: Option[String]
                                   ): Future[ShipmentResponseDto] = {

    val now = Instant.now()

    for {
      shipmentOpt <- readRepo.findByTrackingNumber(trackingNumber)

      shipment <- shipmentOpt match {
        case Some(s) => Future.successful(s)
        case None =>
          Future.failed(new NoSuchElementException(
            s"Shipment with tracking number $trackingNumber not found"
          ))
      }

      // Validate transition via injected validation
      _ <- validation.validateTransition(shipment.status, status) match {
        case Left(err) => Future.failed(new IllegalStateException(err))
        case Right(_)  => Future.successful(())
      }

      updated = shipment.copy(
        status = status,
        history = shipment.history :+ TrackingEvent(
          status = status,
          timestamp = now,
          location = location
        )
      )

      _ <- writeRepo.update(updated)

      result <- readRepo.findByTrackingNumber(trackingNumber)

    } yield ShipmentMapper.toDto(result.getOrElse(updated))
  }




  // LIST ALL
  override def listShipments(): Future[Seq[ShipmentResponseDto]] = {
    readRepo.listAll().map(_.map(ShipmentMapper.toDto))
  }

    // DELETE

  override def deleteShipment(id: java.util.UUID): Future[Int] =
    writeRepo.delete(id)
}
