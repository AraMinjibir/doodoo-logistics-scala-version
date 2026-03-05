package domain.services.impl

import com.google.inject.{Inject, Singleton}
import domain.gateways.{PaymentGateway, PaymentWebhookEvent}
import domain.models.{Payment, PaymentMethod, PaymentStatus}
import domain.services.PaymentService
import repositories.PaymentRepository

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class PaymentServiceImpl @Inject()(
                                    paymentRepository: PaymentRepository,
                                    gateway: PaymentGateway)
                                  (implicit ec: ExecutionContext)
                                  extends PaymentService {

  override def initiatePayment(
                                payment: Payment,
                                callbackUrl: String
                              ): Future[String] = {

    for {

      // 1 Prevent duplicate payment
      existing <- paymentRepository.getPaymentById(payment.referenceNumber)

      _ <- existing match {
        case Some(_) =>
          Future.failed(
            new IllegalStateException(
              s"Payment already exists for ${payment.referenceNumber}"
            )
          )

        case None =>
          paymentRepository.makePayment(payment).map(_ => ())
      }

      // 2 Call payment gateway
      gatewayResponse <- gateway.initiatePayment(payment, callbackUrl)

    } yield gatewayResponse.authorizationUrl
  }
  override def handleWebhook(
                              payload: String,
                              signature: String
                            ): Future[Either[String, Payment]] = {

    gateway.verifyWebhook(payload, signature) match {

      case Left(error) =>
        Future.successful(Left(error))

      case Right(event) =>
        processWebhookEvent(event)
    }
  }
  private def processWebhookEvent(
                                   event: PaymentWebhookEvent
                                 ): Future[Either[String, Payment]] = {

    paymentRepository.getPaymentById(event.reference).flatMap {

      case None =>
        Future.successful(Left("Payment not found"))

      case Some(payment) =>

        val newStatus =
          if (event.status == "success")
            PaymentStatus.Successful
          else
            PaymentStatus.Failed

        if (!Payment.canTransition(payment.status, newStatus)) {

          Future.successful(
            Left(
              s"Invalid payment state transition ${payment.status} → $newStatus"
            )
          )

        } else {

          val updatedPayment =
            payment.copy(status = newStatus)

          paymentRepository
            .updatePayment(updatedPayment)
            .map(_ => Right(updatedPayment))
        }

    }
  }
  override def getPaymentById(ref:String):Future[Option[Payment]] = paymentRepository.getPaymentById(ref)

  override def getPaymentByMethod(method: PaymentMethod): Future[Seq[Payment]] = paymentRepository.getPaymentByMethod(method)
  override def getPaymentStatus(status:PaymentStatus): Future[Seq[Payment]] = paymentRepository.getPaymentStatus(status)
  override def getAllPayment: Future[Seq[Payment]] = paymentRepository.getAllPayment
  override def deletePayment(ref: String): Future[Either[String, Unit]] = {
    paymentRepository.deletePayment(ref).map{
      case Success(0) => Left(s"No payment with this id:$ref")
      case Success(_) => Right(())
    }
  }

  override def getWeeklyRevenue(date: LocalDate): Future[BigDecimal] = paymentRepository.getWeeklyRevenue(date)
  override def getDailyRevenue(date:LocalDate):Future[BigDecimal] = paymentRepository.getDailyRevenue(date)
  override def getMonthlyRevenue(year: Int, month: Int): Future[BigDecimal] = paymentRepository.getMonthlyRevenue(year, month)

}
