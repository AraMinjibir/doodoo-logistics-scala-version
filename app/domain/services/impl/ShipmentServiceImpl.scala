package domain.services.impl

import api.dto.{CreateShipmentDto, ShipmentDto}
import domain.models.{ShipmentStatus, TrackingEvent}
import domain.services.ShipmentService
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
                                     readRepo: ShipmentReadRepository
                                   )(implicit ec: ExecutionContext) extends ShipmentService {


  // CREATE

  override def createShipment(dto: CreateShipmentDto): Future[ShipmentDto] = {
    // Basic validation
    if (dto.senderName.trim.isEmpty) {
      Future.failed(new IllegalArgumentException("senderName is required"))
    } else if (dto.recipientName.trim.isEmpty) {
      Future.failed(new IllegalArgumentException("recipientName is required"))
    } else {
      val now = Instant.now()

      // Build domain from DTO (mapper returns a domain Shipment with id, history.)
      val baseDomain = ShipmentMapper.toDomain(dto)

      // Generate tracking number BEFORE saving so caller receives it deterministically
      val tracking = TrackingNumberGenerator.generate()

      // Ensure we set tracking number and createdAt consistently
      val domainWithTracking = baseDomain.copy(
        trackingNumber = Some(tracking),
        createdAt = baseDomain.createdAt match {
          case ts if ts != null => ts
          case _ => now
        }
      )

      // Persist and read back authoritative record
      for {
        // insert; we ignore the Int result (rows affected)
        _       <- writeRepo.create(domainWithTracking)

        // read back by tracking number
        savedOpt <- readRepo.findByTrackingNumber(tracking)

        saved = savedOpt.getOrElse(domainWithTracking) // fallback: return domain we created if read fails

        dtoOut = ShipmentMapper.toDto(saved)
      } yield dtoOut
    }
  }
  // GET BY TRACKING NUMBER

  override def getShipmentByTrackingNumber(trackingNumber: String): Future[Option[ShipmentDto]] = {
    readRepo
      .findByTrackingNumber(trackingNumber)
      .map(_.map(ShipmentMapper.toDto))
  }

  // --------------------------------------
  // UPDATE STATUS
  // --------------------------------------
  override def updateShipmentStatus(
                                     trackingNumber: String,
                                     status: ShipmentStatus,
                                     location: Option[String]
                                   ): Future[ShipmentDto] = {

    val now = Instant.now()

    for {
      // 1. Load shipment
      shipmentOpt <- readRepo.findByTrackingNumber(trackingNumber)

      shipment <- shipmentOpt match {
        case Some(s) => Future.successful(s)
        case None    => Future.failed(
          new NoSuchElementException(
            s"Shipment with tracking number $trackingNumber not found"
          )
        )
      }

      // 2. Prepare the updated shipment with correct history append
      updated = shipment.copy(
        status = status,
        history = shipment.history :+ TrackingEvent(
          status = status,      // use the new status.
          timestamp = now,
          location = location
        )
      )

      // 3. Persist change
      _ <- writeRepo.update(updated)

      // 4. Re-fetch to ensure final state from DB
      result <- readRepo.findByTrackingNumber(trackingNumber)

    } yield ShipmentMapper.toDto(result.getOrElse(updated))
  }


  // LIST ALL
  override def listShipments(): Future[Seq[ShipmentDto]] = {
    readRepo.listAll().map(_.map(ShipmentMapper.toDto))
  }

    // DELETE

  override def deleteShipment(id: java.util.UUID): Future[Int] =
    writeRepo.delete(id)
}
