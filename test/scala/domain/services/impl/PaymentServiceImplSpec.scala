package scala.domain.services.impl

import domain.gateways.{PaymentGateway, PaymentGatewayResponse, PaymentWebhookEvent}
import domain.models.{Payment, PaymentMethod, PaymentStatus, Shipment}
import domain.services.EventBus
import domain.services.impl.PaymentServiceImpl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.{PaymentRepository, ShipmentRepository}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.helpers.{PaymentTestHelper, ShipmentTestHelpers}
import scala.util.Success

class PaymentServiceImplSpec extends AnyWordSpec
  with Matchers
  with MockitoSugar
  with PaymentTestHelper
  with ShipmentTestHelpers{

  implicit val ec: ExecutionContext = ExecutionContext.global


  val paymentRepository: PaymentRepository = mock[PaymentRepository]
  val shipmentRepository: ShipmentRepository = mock[ShipmentRepository]
  val eventBus: EventBus = mock[EventBus]
  val gateway: PaymentGateway = mock[PaymentGateway]

  val service =
    new PaymentServiceImpl(paymentRepository: PaymentRepository,
      shipmentRepository:ShipmentRepository,
      gateway: PaymentGateway, eventBus)

  val payment: Payment = samplePayment
  val shipment: Shipment = createTestShipment()


  "PaymentServiceImpl" should{
    "initiatePayment" should {
      "return authorization url when payment is valid" in {

        val callbackUrl = "https://callback.test"

        when(shipmentRepository.getById(payment.shipmentId))
          .thenReturn(Future.successful(Some(shipment)))

        when(paymentRepository.getPaymentByShipmentId(payment.shipmentId))
          .thenReturn(Future.successful(None))

        when(gateway.initiatePayment(payment, callbackUrl))
          .thenReturn(
            Future.successful(
              PaymentGatewayResponse(
                authorizationUrl = "https://pay.test/123",
                reference = payment.referenceNumber
              )
            )
          )

        service.initiatePayment(payment, callbackUrl).map { result =>
          result.isRight mustBe true
        }

      }
      "fail if payment already exists" in {

        val callbackUrl = "https://callback.test"

        when(shipmentRepository.getById(payment.shipmentId))
          .thenReturn(Future.successful(Some(shipment)))

        when(paymentRepository.getPaymentByShipmentId(payment.shipmentId))
          .thenReturn(Future.successful(Some(payment)))

        service.initiatePayment(payment, callbackUrl).map { result =>

          result.isLeft mustBe true

        }
      }

      "handleWebhook" should {

        "update payment to successful when webhook status is success" in {

          val event =
            PaymentWebhookEvent(
              reference = payment.referenceNumber,
              status = "success",
              gatewayTransactionId = Some("TX123")
            )

          when(gateway.verifyWebhook("payload", "signature"))
            .thenReturn(Right(event))

          when(paymentRepository.getPaymentById(payment.referenceNumber))
            .thenReturn(Future.successful(Some(payment)))

          when(paymentRepository.updatePayment(any()))
            .thenReturn(Future.successful(Success(1)))

          service
            .handleWebhook("payload", "signature")
            .map { result =>

              result.isRight mustBe true

              val updated = result.toOption.get

              updated.status mustBe PaymentStatus.Successful
            }
        }
        "return error if payment does not exist" in {

          val event =
            PaymentWebhookEvent(
              reference = payment.referenceNumber,
              status = "success",
              gatewayTransactionId = None
            )

          when(gateway.verifyWebhook("payload", "signature"))
            .thenReturn(Right(event))

          when(paymentRepository.getPaymentById(payment.referenceNumber))
            .thenReturn(Future.successful(None))

          service
            .handleWebhook("payload", "signature")
            .map { result =>

              result mustBe Left("Payment not found")
            }
        }
        "deletePayment" should {

          "delete payment successfully" in {

            when(paymentRepository.deletePayment(payment.referenceNumber))
              .thenReturn(Future.successful(Success(1)))

            service
              .deletePayment(payment.referenceNumber)
              .map { result =>

                result mustBe Right(())
              }
          }

          "return error if payment does not exist" in {

            when(paymentRepository.deletePayment(payment.referenceNumber))
              .thenReturn(Future.successful(Success(0)))

            service
              .deletePayment(payment.referenceNumber)
              .map { result =>

                result mustBe Left(s"No payment with this id:${payment.referenceNumber}")
              }
          }
        }
      }


    }
    "getPaymentById" should {

      "return payment when repository finds one" in {

        val payment = samplePayment

        when(paymentRepository.getPaymentById(payment.referenceNumber))
          .thenReturn(Future.successful(Some(payment)))

        service.getPaymentById(payment.referenceNumber).map { result =>

          result mustBe Some(payment)

          verify(paymentRepository).getPaymentById(payment.referenceNumber)
          succeed
        }
      }

      "return None when payment does not exist" in {

        when(paymentRepository.getPaymentById("unknown-ref"))
          .thenReturn(Future.successful(None))

        service.getPaymentById("unknown-ref").map { result =>

          result mustBe None

          verify(paymentRepository).getPaymentById("unknown-ref")

          succeed
        }
      }

    }
    "getPaymentByMethod" should {
      "return payments filtered by method" in {

        val payments = Seq(samplePayment)

        when(paymentRepository.getPaymentByMethod(PaymentMethod.Card))
          .thenReturn(Future.successful(payments))

        service.getPaymentByMethod(PaymentMethod.Card).map { result =>

          result mustBe payments

          verify(paymentRepository).getPaymentByMethod(PaymentMethod.Card)

          succeed
        }
      }
    }
    "getPaymentStatus" should {

      "return payments by status" in {

        val payments = Seq(samplePayment)

        when(paymentRepository.getPaymentStatus(PaymentStatus.Successful))
          .thenReturn(Future.successful(payments))

        service.getPaymentStatus(PaymentStatus.Successful).map { result =>

          result mustBe payments

          verify(paymentRepository).getPaymentStatus(PaymentStatus.Successful)

          succeed
        }
      }
    }
    "getAllPayment" should {

      "return all payments" in {

        val payments = Seq(samplePayment)

        when(paymentRepository.getAllPayment)
          .thenReturn(Future.successful(payments))

        service.getAllPayment.map { result =>

          result mustBe payments

          verify(paymentRepository).getAllPayment

          succeed
        }
      }
    }
    "deletePayment" should {

      "return Right when payment is deleted" in {
        val ref = "payment-ref"
        when(paymentRepository.deletePayment(ref))
          .thenReturn(Future.successful(Success(1)))

        service.deletePayment(ref).map { result =>
          val ref = "payment-ref"
          result mustBe Right(())

          verify(paymentRepository).deletePayment(ref)

          succeed
        }
      }
    }
    "return Left when payment does not exist" in {

      when(paymentRepository.deletePayment("unknown-ref"))
        .thenReturn(Future.successful(Success(0)))

      service.deletePayment("unknown-ref").map { result =>

        result mustBe Left("No payment with this id:unknown-ref")

        verify(paymentRepository).deletePayment("unknown-ref")

        succeed
      }
    }
    "Return a Revenue" should {
      "return daily revenue" in {

        val date = LocalDate.now()

        when(paymentRepository.getDailyRevenue(date))
          .thenReturn(Future.successful(BigDecimal(10000)))

        service.getDailyRevenue(date).map { result =>

          result mustBe BigDecimal(10000)

          verify(paymentRepository).getDailyRevenue(date)

          succeed
        }
      }
      "return weekly revenue" in {

        val date = LocalDate.now()

        when(paymentRepository.getWeeklyRevenue(date))
          .thenReturn(Future.successful(BigDecimal(50000)))

        service.getWeeklyRevenue(date).map { result =>

          result mustBe BigDecimal(50000)

          verify(paymentRepository).getWeeklyRevenue(date)

          succeed
        }
      }
      "return monthly revenue" in {

        when(paymentRepository.getMonthlyRevenue(2026, 4))
          .thenReturn(Future.successful(BigDecimal(200000)))

        service.getMonthlyRevenue(2026, 4).map { result =>

          result mustBe BigDecimal(200000)

          verify(paymentRepository).getMonthlyRevenue(2026, 4)

          succeed
        }
      }
    }
  }
}
