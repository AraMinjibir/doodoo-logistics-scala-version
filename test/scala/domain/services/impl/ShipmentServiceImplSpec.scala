package scala.domain.services.impl

import api.dto.{CreateShipmentDto, DimensionsDto, PackageDetailsDto, RecipientDto}
import domain.models._
import domain.models.ShipmentStatus.{Created, Delivered, Pending}
import domain.services.impl.ShipmentServiceImpl
import domain.validation.ShipmentValidation
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.read.ShipmentReadRepository
import repositories.write.ShipmentWriteRepository
import utilities.{CostCalculator, DateEstimator}

import java.time.Instant
import java.util.UUID
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ShipmentServiceImplSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar with BeforeAndAfterEach  {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(
      mockReadRepo,
      mockWriteRepo,
      mockValidation,
      mockCostCalculator,
      mockDateEstimator
    )
  }

  // Top-Level Test Fixtures (Shared Across All Tests)

  val validCreateDto: CreateShipmentDto =
    CreateShipmentDto(
      senderName = "Ara Minjibir",
      recipient = RecipientDto(
        name = "Test Recipient",
        address = Address(
          street = "123 Main St",
          city = "Mjb",
          state = "Kano",
          country = "Nigeria",
          postalCode = "100001"
        ),
        contact = "08012345678"
      ),
      packageDetails = PackageDetailsDto(
        weight = 30.0,
        dimensions = DimensionsDto(30.0, 20.0, 10.0),
        contents = "Clothing"
      )
    )

  val shipmentId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val trackingNumber: String = "GET-TRACK-123"
  val fixedInstant: Instant = Instant.parse("2026-01-10T10:00:00Z")

  val recipientDomain: Recipient =
    Recipient(
      name = validCreateDto.recipient.name,
      address = validCreateDto.recipient.address,
      contact = validCreateDto.recipient.contact
    )

  val packageDetailsDomain: PackageDetails =
    PackageDetails(
      weight = validCreateDto.packageDetails.weight,
      dimensions = Dimensions(
        length = validCreateDto.packageDetails.dimensions.length,
        width  = validCreateDto.packageDetails.dimensions.width,
        height = validCreateDto.packageDetails.dimensions.height
      ),
      contents = validCreateDto.packageDetails.contents
    )

  val existingShipment: Shipment =
    Shipment(
      id = shipmentId,
      senderName = validCreateDto.senderName,
      recipient = recipientDomain,
      packageDetails = packageDetailsDomain,
      cost = BigDecimal(5000),
      estimatedDeliveryDate = None,
      trackingNumber = Some(trackingNumber),
      status = ShipmentStatus.Created,
      createdAt = fixedInstant,
      history = Nil,
      updatedAt = fixedInstant
    )

  // Mocks

  val mockReadRepo: ShipmentReadRepository     = mock[ShipmentReadRepository]
  val mockWriteRepo: ShipmentWriteRepository   = mock[ShipmentWriteRepository]
  val mockValidation: ShipmentValidation       = mock[ShipmentValidation]
  val mockCostCalculator: CostCalculator       = mock[CostCalculator]
  val mockDateEstimator: DateEstimator         = mock[DateEstimator]

  val service =
    new ShipmentServiceImpl(
      mockWriteRepo,
      mockReadRepo,
      mockValidation,
      mockCostCalculator,
      mockDateEstimator
    )

  // Tests

  "ShipmentServiceImpl" should {

    "1. CREATE a shipment successfully when input is valid" in {

      when(mockValidation.validateCreate(any[CreateShipmentDto]))
        .thenReturn(Right(()))

      when(mockWriteRepo.create(any[Shipment]))
        .thenReturn(Future.successful(1))

      when(mockCostCalculator.calculate(any(), any()))
        .thenReturn(BigDecimal(5000))

      when(mockDateEstimator.estimate(any()))
        .thenReturn(Some(Instant.parse("2026-01-01T12:00:00Z")))

      val result =
        Await.result(service.createShipment(validCreateDto), 5.seconds)

      result.senderName shouldBe validCreateDto.senderName
      result.cost shouldBe BigDecimal(5000)
      result.estimatedDeliveryDate shouldBe Some(
        Instant.parse("2026-01-01T12:00:00Z")
      )
      result.trackingNumber shouldBe defined

      verify(mockWriteRepo, times(1)).create(any[Shipment])
    }

    "2. UPDATE shipment status successfully" in {

      when(mockReadRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(Some(existingShipment)))

      when(mockValidation.validateTransition(Created, Delivered))
        .thenReturn(Right(()))

      when(mockWriteRepo.update(any[Shipment]))
        .thenReturn(Future.successful(1))

      val resultEither =
        Await.result(
          service.updateShipmentStatus(
            trackingNumber,
            ShipmentStatus.Delivered,
            Some("Minjibir Hub")
          ),
          5.seconds
        )

      resultEither.isRight shouldBe true

      val result = resultEither.getOrElse(fail("Expected a Right but got a Left"))

      result.status shouldBe ShipmentStatus.Delivered
      result.history.last.status shouldBe ShipmentStatus.Delivered
      result.trackingNumber shouldBe Some(trackingNumber)

      verify(mockWriteRepo, times(1)).update(any[Shipment])
      verify(mockReadRepo, times(1)).findByTrackingNumber(trackingNumber)
      verify(mockValidation, times(1))
        .validateTransition(Created, Delivered)
    }

    "3. GET shipment by tracking number successfully" in {

      when(mockReadRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(Some(existingShipment)))

      val result =
        Await.result(
          service.getShipmentByTrackingNumber(trackingNumber),
          5.seconds
        )

      result shouldBe defined

      val dto = result.get
      dto.trackingNumber shouldBe Some(trackingNumber)
      dto.status shouldBe ShipmentStatus.Created
      dto.senderName shouldBe validCreateDto.senderName

      verify(mockReadRepo, times(1)).findByTrackingNumber(trackingNumber)
    }

    "4. RETURN None when shipment does not exist" in {

      when(mockReadRepo.findByTrackingNumber("MISSING-TRACK-999"))
        .thenReturn(Future.successful(None))

      val result =
        Await.result(
          service.getShipmentByTrackingNumber("MISSING-TRACK-999"),
          5.seconds
        )

      result shouldBe None
      verify(mockReadRepo, times(1))
        .findByTrackingNumber("MISSING-TRACK-999")
    }

    "5 GET shipment by id successfully" in {
//      Define Mock

      when(mockReadRepo.getById(shipmentId))
        .thenReturn(Future.successful(Some(existingShipment)))

//      Act
      val result = Await.result(service.getShipmentById(shipmentId), 5.seconds)

//      Assert
      result shouldBe defined

      val dto = result.get
      dto.id shouldBe (shipmentId)

      verify(mockReadRepo, times(1)).getById(shipmentId)

    }

    "6 RETURN None when shipment does not exist" in {

      val missingId = UUID.fromString("22222222-2222-2222-2222-222222222222")

      when(mockReadRepo.getById(missingId))
        .thenReturn(Future.successful(None))

      val result = Await.result(service.getShipmentById(missingId),5.seconds )

      result shouldBe None

      verify(mockReadRepo, times(1))
        .getById(missingId)
    }

    "7 GET shipment by status successfully" in{
      val shipmentStatus:ShipmentStatus = Created

      when(mockReadRepo.getByStatus(shipmentStatus))
        .thenReturn(Future.successful(Seq(existingShipment)))

      val result = Await.result(service.getShipmentByStatus(shipmentStatus), 5.seconds)

      result should not be empty

      val dto = result.head
      dto.status shouldBe shipmentStatus

      verify(mockReadRepo, times(1)).getByStatus(shipmentStatus)
    }

    "8 RETURN Empty when shipment does not exist" in{

      val fakeStatus:ShipmentStatus = Delivered

      when(mockReadRepo.getByStatus(fakeStatus))
        .thenReturn(Future.successful(Seq.empty))
      val result = Await.result(service.getShipmentByStatus(fakeStatus), 5.seconds)

      result shouldBe empty
      verify(mockReadRepo, times(1)).getByStatus(fakeStatus)

    }

    "9. GET All shipment  successfully" in{
       when(mockReadRepo.listAll())
         .thenReturn(Future.successful(Seq(existingShipment)))

      val result = Await.result(service.listShipments(), 5.seconds)

      result should not be empty

      verify(mockReadRepo, times(1)).listAll()
    }

    "10 RETURN Empty when shipment does not exist" in{
      when(mockReadRepo.listAll())
        .thenReturn(Future.successful(Seq.empty))

      val result =  Await.result(service.listShipments(), 5.seconds)

      result shouldBe empty

      verify(mockReadRepo, times(1)).listAll()
    }

    "11 Delete shipment successfully" in {
      when(mockWriteRepo.delete(shipmentId))
        .thenReturn(Future.successful(1))

      val result = Await.result(service.deleteShipment(shipmentId), 5.seconds)

      result shouldBe Right(())
      verify(mockWriteRepo, times(1)).delete(shipmentId)
    }

    "12 RETURN False when shipment is not deleted" in {
      when(mockWriteRepo.delete(shipmentId))
        .thenReturn(Future.successful(0))

      val result = Await.result(service.deleteShipment(shipmentId), 5.seconds)

      result shouldBe Left(s"Shipment with ID $shipmentId not found or already deleted")
      verify(mockWriteRepo,times(1)).delete(shipmentId)
    }

    "13. FAIL to create a shipment when validation fails" in {

      // --- Arrange ---

      when(mockValidation.validateCreate(any[CreateShipmentDto]))
        .thenReturn(Left("In valid shipment data"))

      // --- Act + Assert ---
      val ex = intercept[Exception] {
        Await.result(
          service.createShipment(validCreateDto),
          5.seconds
        )
      }

//      Assert
     ex.getMessage should  include ("In valid shipment data")

      // --- Behavioral guarantees ---
      verify(mockWriteRepo, never()).create(any[Shipment])
      verify(mockCostCalculator, never()).calculate(any(), any())
      verify(mockDateEstimator, never()).estimate(any())
    }

    "14 FAIL when shipment is not found" in {

      when(mockReadRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(None))

      val ex = recoverToExceptionIf[NoSuchElementException] {
        service.updateShipmentStatus(trackingNumber, ShipmentStatus.InTransit, Some("Mjb"))
      }

      ex.map { e =>
        e.getMessage should include ("Shipment not found")

        verify(mockReadRepo, times(1)).findByTrackingNumber(trackingNumber)
        verify(mockWriteRepo, never()).update(any())
      }
    }

    "15 FAIL when status transition is invalid" in {

      val deliveredShipment =
        existingShipment.copy(status = ShipmentStatus.Delivered)

      when(mockReadRepo.findByTrackingNumber(trackingNumber))
        .thenReturn(Future.successful(Some(deliveredShipment)))

      val ex = recoverToExceptionIf[IllegalStateException] {
        service.updateShipmentStatus(trackingNumber, ShipmentStatus.Created, Some(""))
      }

      ex.map { e =>
        e.getMessage should include ("Invalid status transition")

        verify(mockReadRepo, times(1)).findByTrackingNumber(trackingNumber)
        verify(mockWriteRepo, never()).update(any())
      }
    }



  }
}
