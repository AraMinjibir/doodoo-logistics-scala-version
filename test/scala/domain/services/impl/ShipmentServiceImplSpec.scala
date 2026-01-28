package scala.domain.services.impl

import domain.models.ShipmentStatus
import domain.models.ShipmentStatus._
import domain.services.impl.ShipmentServiceImpl
import domain.validation.ShipmentValidation
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.ShipmentRepository
import utilities.DateEstimator

import java.util.UUID
import scala.domain.helpers.ShipmentTestHelpers
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try,Failure,Success}

class ShipmentServiceImplSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with ShipmentTestHelpers {

  // Mocks

  val mockRepo      = mock[ShipmentRepository]
  val mockValidation     = mock[ShipmentValidation]
  val mockCostCalculator = mock[CostCalculator]
  val mockDateEstimator  = mock[DateEstimator]
  override val shipmentId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

  val service = new ShipmentServiceImpl(
    mockRepo, mockValidation, mockCostCalculator, mockDateEstimator
  )

  override def beforeEach(): Unit = {
    reset(mockRepo, mockRepo, mockValidation, mockCostCalculator, mockDateEstimator)
  }

  "ShipmentServiceImpl" should {

    "1. CREATE a shipment successfully when input is valid" in {
      when(mockValidation.validateCreate(any())).thenReturn(Right(()))
      when(mockRepo.create(any())).thenReturn(Future.successful(Success(1)))
      when(mockCostCalculator.calculate(any(), any())).thenReturn(BigDecimal(5000))
      when(mockDateEstimator.estimate(any())).thenReturn(Some(fixedInstant))

      val result = Await.result(service.createShipment(validCreateDto()), 5.seconds)

      result match {
        case Right(shipment) => shipment.cost shouldBe BigDecimal(5000)
        case Left(err) =>  fail(s"Expected success, but got error: $err")
      }
      verify(mockRepo).create(any())
    }

    "2. UPDATE shipment status successfully" in {
      val existing = createTestShipment(status = Created)
      when(mockRepo.findByTrackingNumber(trackingNumber)).thenReturn(Future.successful(Some(existing)))
      when(mockValidation.validateTransition(Created, Delivered)).thenReturn(Right(()))
      when(mockRepo.update(any())).thenReturn(Future.successful(Success(1)))

      val result = Await.result(service.updateShipmentStatus(trackingNumber, Delivered, Some("Hub")), 5.seconds)

      result.map(_.status) shouldBe Right(Delivered)
    }

    "3. GET shipment by tracking number successfully" in {
      // Using 'trackingNumber' constant from the trait
      when(mockRepo.findByTrackingNumber(trackingNumber)).thenReturn(Future.successful(Some(createTestShipment())))
      val result = Await.result(service.getShipmentByTrackingNumber(trackingNumber), 5.seconds)

      // Assert using the constant 'trackingNumber' instead of a hardcoded string
      result.flatMap(_.trackingNumber) shouldBe Some(trackingNumber)
    }

    "4. RETURN None when shipment does not exist by tracking" in {
      when(mockRepo.findByTrackingNumber(any())).thenReturn(Future.successful(None))
      val result = Await.result(service.getShipmentByTrackingNumber("MISSING"), 5.seconds)
      result shouldBe None
    }

    "5. GET shipment by id successfully" in {
      // Using 'shipmentId' constant from the trait
      when(mockRepo.getById(shipmentId)).thenReturn(Future.successful(Some(createTestShipment())))
      val result = Await.result(service.getShipmentById(shipmentId), 5.seconds)

      result.map(_.id) shouldBe Some(shipmentId)
    }

    "6. RETURN None when shipment does not exist by id" in {
      when(mockRepo.getById(any())).thenReturn(Future.successful(None))
      val result = Await.result(service.getShipmentById(UUID.randomUUID()), 5.seconds)
      result shouldBe None
    }

    "7. GET shipment by status successfully" in {
      when(mockRepo.getByStatus(Created)).thenReturn(Future.successful(Seq(createTestShipment(status = Created))))
      val result = Await.result(service.getShipmentByStatus(Created), 5.seconds)
      result should not be empty
    }

    "8. RETURN Empty when shipment does not exist by status" in {
      when(mockRepo.getByStatus(any())).thenReturn(Future.successful(Seq.empty))
      val result = Await.result(service.getShipmentByStatus(Delivered), 5.seconds)
      result shouldBe empty
    }

    "9. GET All shipments successfully" in {
      when(mockRepo.listAll(0, 10)).thenReturn(Future.successful(Seq(createTestShipment())))
      val result = Await.result(service.listShipments(0, 10), 5.seconds)
      result should have size 1
    }

    "10. RETURN Empty when no shipments exist" in {
      when(mockRepo.listAll(0, 10)).thenReturn(Future.successful(Seq.empty))
      val result = Await.result(service.listShipments(0, 10), 5.seconds)
      result shouldBe empty
    }

    "11. DELETE shipment successfully" in {
      when(mockRepo.delete(shipmentId)).thenReturn(Future.successful(Success(1)))
      val result = Await.result(service.deleteShipment(shipmentId), 5.seconds)
      result shouldBe Right(())
    }

    "12. RETURN Left when shipment is not found for deletion" in {
      when(mockRepo.delete(shipmentId)).thenReturn(Future.successful(Success(0)))
      val result = Await.result(service.deleteShipment(shipmentId), 5.seconds)
      result shouldBe Left("Shipment not found")
    }

    "13. FAIL to create a shipment when validation fails" in {
      when(mockValidation.validateCreate(any())).thenReturn(Left("Validation Error"))
      val ex = intercept[Exception] {
        Await.result(service.createShipment(validCreateDto()), 5.seconds)
      }
      ex.getMessage should include ("Validation Error")
    }

      "14 FAIL when shipment is not found" in {

        when(mockRepo.findByTrackingNumber(trackingNumber))
          .thenReturn(Future.successful(None))

        val ex = recoverToExceptionIf[NoSuchElementException] {
          service.updateShipmentStatus(trackingNumber, ShipmentStatus.InTransit, Some("Mjb"))
        }

        ex.map { e =>
          e.getMessage should include ("Shipment not found")

          verify(mockRepo, times(1)).findByTrackingNumber(trackingNumber)
          verify(mockRepo, never()).update(any())
        }
      }

    "15. FAIL when status transition is invalid" in {
      // Arrange
      val delivered = createTestShipment(status = Delivered)
      when(mockRepo.findByTrackingNumber(trackingNumber)).thenReturn(Future.successful(Some(delivered)))

      val errorMsg = "Cannot transition from Delivered to Created"
      when(mockValidation.validateTransition(Delivered, Created)).thenReturn(Left(errorMsg))

      // Act
      val result = Await.result(service.updateShipmentStatus(trackingNumber, Created, None), 5.seconds)

      // Assert
      result shouldBe Left(errorMsg)
      verify(mockRepo, never()).update(any())
    }
  }
}