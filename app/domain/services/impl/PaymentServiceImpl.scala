package domain.services.impl

import com.google.inject.{Inject, Singleton}
import domain.errors.{DomainError, DuplicateError}
import domain.gateways.{PaymentGateway, PaymentGatewayResponse, PaymentWebhookEvent}
import domain.models.{Payment, PaymentFailed, PaymentMethod, PaymentStatus, PaymentSucceeded}
import domain.services.{EventBus, PaymentService}
import repositories.{PaymentRepository, ShipmentRepository}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class PaymentServiceImpl @Inject()(
                                    paymentRepository: PaymentRepository,
                                    shipmentRepository:ShipmentRepository,
                                    gateway: PaymentGateway,
                                    eventBus: EventBus)
                                  (implicit ec: ExecutionContext)
                                  extends PaymentService {

  override def initiatePayment(
                                payment: Payment,
                                callbackUrl: String
                              ): Future[Either[DomainError, PaymentGatewayResponse]] = {

    // 1. Ensure shipment exists
    shipmentRepository.getById(payment.shipmentId).flatMap {
      case None =>
        // Shipment does not exist → return Left
        Future.successful(Left(DuplicateError(s"Shipment ${payment.shipmentId} not found")))

      case Some(_) =>
        // 2. Prevent duplicate payment
        paymentRepository.getPaymentByShipmentId(payment.shipmentId).flatMap {
          case Some(existing) =>
            Future.successful(Left(DuplicateError(s"Payment already initiated: ${existing.referenceNumber}")))

          case None =>
            // 3. Call payment gateway and wrap in Right
            gateway.initiatePayment(payment, callbackUrl).flatMap { gatewayResp =>

              val paymentToSave = payment.copy(
                referenceNumber = gatewayResp.reference,
                status = PaymentStatus.Pending
              )

              paymentRepository.savePayment(paymentToSave).map { _ =>
                Right(gatewayResp)
              }
            }
        }
    }
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
            .map { _ =>

              newStatus match {

                case PaymentStatus.Successful =>
                  eventBus.publish(
                    PaymentSucceeded(
                      reference = updatedPayment.referenceNumber,
                      email = updatedPayment.customerId.toString,
                      amount = updatedPayment.amount
                    )
                  )

                case PaymentStatus.Failed =>
                  eventBus.publish(
                    PaymentFailed(
                      reference = updatedPayment.referenceNumber,
                      email = updatedPayment.customerId.toString,
                      reason = event.status
                    )
                  )

                case _ => ()
              }

              Right(updatedPayment)
            }

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
