package scala.domain.services.impl

import domain.errors.{ShipmentNotDelivered, ShipmentNotFound}
import domain.models.UserRole.Recipient
import domain.models.{Shipment, ShipmentStatus, UserRole}
import domain.services.EventBus
import domain.services.impl.ShipmentServiceImpl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.{ShipmentRepository, UserRepository}

import java.util.UUID
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.helpers.ShipmentTestHelpers
import scala.util.{Failure, Success, Try}

class ShipmentServiceImplSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with ShipmentTestHelpers {

  // Mocks

  val mockRepo: ShipmentRepository = mock[ShipmentRepository]
  val mockUserRepo: UserRepository = mock[UserRepository]
  val eventBus:EventBus = mock[EventBus]


  override val shipmentId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

  val service = new ShipmentServiceImpl(mockRepo,mockUserRepo,eventBus)

  override def beforeEach(): Unit = {
    reset(mockRepo)
  }
val shipment: Shipment = validShipment()

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

      result shouldBe Right(())
    }

    "UPLOAD proof of delivery successfully when the shipment status is delivered" in {
      val proof = validProof
      val deliveredShipment = shipment.copy(status = ShipmentStatus.Delivered)

      when(mockRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(Some(deliveredShipment)))

      when(mockRepo.uploadProofOfDelivery(shipmentId,proof))
        .thenReturn(Future.successful(Some(deliveredShipment)))

      val result = Await.result(service.uploadProofOfDelivery(trackingNumber,proof), 5.second)

      result.map(_.status) shouldBe Right(ShipmentStatus.Delivered)
      verify(mockRepo).uploadProofOfDelivery(shipmentId,proof)


    }
    "Fail to upload the proof of delivery when shipment status is not Delivered" in {

      val notDeliveredShipment =
        shipment.copy(status = ShipmentStatus.Created)

      when(mockRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(Some(notDeliveredShipment)))

      val result =
        Await.result(service.uploadProofOfDelivery(trackingNumber, validProof), 5.seconds)

      result shouldBe Left(List(ShipmentNotDelivered))

      verify(mockRepo, never())
        .uploadProofOfDelivery(shipmentId, validProof)
    }

    "ASSIGN service provider to shipment" should {

      "successfully assign a service provider" in {
        // Arrange
        val shipmentInDb = shipment
        val serviceProviderUser = serviceProvider.copy(id = UUID.randomUUID())

        when(mockRepo.getById(shipment.id))
          .thenReturn(Future.successful(Some(shipmentInDb)))

        when(mockUserRepo.findUserById(serviceProviderUser.id))
          .thenReturn(Future.successful(Some(serviceProviderUser)))

        when(mockRepo.assignServiceProvider(shipment.id, serviceProviderUser.id))
          .thenReturn(Future.successful(Success(1)))

        // Act
        val result = Await.result(
          service.assignServiceProviderToShipment(shipment.id, serviceProviderUser.id),
          5.seconds
        )

        // Assert
        result match {
          case Right(updatedShipment) =>
            updatedShipment.serviceProviderId shouldBe Some(serviceProviderUser.id)
            updatedShipment.status shouldBe ShipmentStatus.Assigned
          case Left(_) => fail("Expected assignment to succeed")
        }

        verify(mockRepo).assignServiceProvider(shipment.id, serviceProviderUser.id)
      }

      "fail if the shipment does not exist" in {
        val unknownShipmentId = UUID.randomUUID()
        val serviceProviderUser = serviceProvider.copy(id = UUID.randomUUID())

        when(mockRepo.getById(unknownShipmentId)).thenReturn(Future.successful(None))

        val result = Await.result(
          service.assignServiceProviderToShipment(unknownShipmentId, serviceProviderUser.id),
          5.seconds
        )

        result match {
          case Left(err) =>
            err.toString should include("ShipmentNotFound")
          case Right(_) => fail("Expected failure for non-existent shipment")
        }

        verify(mockRepo, never()).assignServiceProvider(any(), any())
      }

      "fail if the user does not exist" in {
        when(mockRepo.getById(shipment.id)).thenReturn(Future.successful(Some(shipment)))
        val unknownUserId = UUID.randomUUID()
        when(mockUserRepo.findUserById(unknownUserId)).thenReturn(Future.successful(None))

        val result = Await.result(
          service.assignServiceProviderToShipment(shipment.id, unknownUserId),
          5.seconds
        )

        result match {
          case Left(err) =>
            err.toString should include("UserNotFoundWithId")
          case Right(_) => fail("Expected failure for non-existent user")
        }

        verify(mockRepo, never()).assignServiceProvider(any(), any())
      }

      "fail if the user is not a service provider" in {
        val nonProviderUser = serviceProvider.copy(role = Recipient)
        when(mockRepo.getById(shipment.id)).thenReturn(Future.successful(Some(shipment)))
        when(mockUserRepo.findUserById(nonProviderUser.id)).thenReturn(Future.successful(Some(nonProviderUser)))

        val result = Await.result(
          service.assignServiceProviderToShipment(shipment.id, nonProviderUser.id),
          5.seconds
        )

        result match {
          case Left(err) =>
            err.toString should include("NotAServiceProvide")
          case Right(_) => fail("Expected failure for user with wrong role")
        }

        verify(mockRepo, never()).assignServiceProvider(any(), any())
      }

    }


  }
}