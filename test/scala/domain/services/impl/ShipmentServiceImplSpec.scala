package scala.domain.services.impl

import domain.errors.ShipmentNotFound
import domain.models.ShipmentStatus
import domain.services.impl.ShipmentServiceImpl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.ShipmentRepository

import java.util.UUID
import scala.domain.helpers.ShipmentTestHelpers
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class ShipmentServiceImplSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with ShipmentTestHelpers {

  // Mocks

  val mockRepo      = mock[ShipmentRepository]
  override val shipmentId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

  val service = new ShipmentServiceImpl(mockRepo)

  override def beforeEach(): Unit = {
    reset(mockRepo)
  }
val shipment = validShipment()
  "ShipmentServiceImpl" should {

    "CREATE a shipment successfully" in {
      when(mockRepo.create(any()))
        .thenReturn(Future.successful(Success(1)))

      val result =
        Await.result(service.createShipment(shipment), 5.seconds)

      result shouldBe Right(shipment)
      verify(mockRepo).create(shipment)
    }

    "RETURN error when repository insert fails" in {
      when(mockRepo.create(any()))
        .thenReturn(Future.successful(Failure(new RuntimeException("DB error"))))

      val result =
        Await.result(service.createShipment(shipment), 5.seconds)

      result.isLeft shouldBe true
    }

    "GET shipment by tracking number" in {
      when(mockRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(Some(shipment)))

      val result =
        Await.result(service.getShipmentByTrackingNumber(trackingNumber), 5.seconds)

      result shouldBe Some(shipment)
    }

    "RETURN None when shipment does not exist by tracking number" in {
      when(mockRepo.findByTrackingNumber(any()))
        .thenReturn(Future.successful(None))

      val result =
        Await.result(service.getShipmentByTrackingNumber("UNKNOWN"), 5.seconds)

      result shouldBe None
    }
    "UPDATE shipment status successfully when transition is valid" in {
      when(mockRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(Some(shipment)))

      when(mockRepo.update(any()))
        .thenReturn(Future.successful(Success(1)))

      val result =
        Await.result(
          service.updateShipmentStatus(
            trackingNumber,
            ShipmentStatus.InTransit,
            Some("Warehouse")
          ),
          5.seconds
        )

      result.map(_.status) shouldBe Right(ShipmentStatus.InTransit)
      verify(mockRepo).update(any())
    }

    "FAIL to update shipment status when shipment does not exist" in {
      when(mockRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(None))

      val result =
        Await.result(
          service.updateShipmentStatus(
            trackingNumber,
            ShipmentStatus.InTransit,
            None
          ),
          5.seconds
        )

      result shouldBe Left(ShipmentNotFound(trackingNumber))
      verify(mockRepo, never()).update(any())
    }

    "FAIL to update shipment status when transition is invalid" in {
      val delivered =
        shipment.copy(status = ShipmentStatus.Delivered)

      when(mockRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(Some(delivered)))

      val result =
        Await.result(
          service.updateShipmentStatus(
            trackingNumber,
            ShipmentStatus.Created,
            None
          ),
          5.seconds
        )

      result.isLeft shouldBe true
      verify(mockRepo, never()).update(any())
    }

    "DELETE shipment successfully" in {
      when(mockRepo.delete(shipmentId))
        .thenReturn(Future.successful(Success(1)))

      val result =
        Await.result(service.deleteShipment(shipmentId), 5.seconds)

      result shouldBe Right(())
    }

    "RETURN Left when deleting non-existing shipment" in {
      when(mockRepo.delete(shipmentId))
        .thenReturn(Future.successful(Success(0)))

      val result =
        Await.result(service.deleteShipment(shipmentId), 5.seconds)

      result shouldBe Left("Shipment not found")
    }
  }}