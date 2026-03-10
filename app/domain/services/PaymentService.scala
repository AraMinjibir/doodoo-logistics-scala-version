package domain.services

import domain.errors.DomainError
import domain.gateways.PaymentGatewayResponse
import domain.models.{Payment, PaymentMethod, PaymentStatus}

import java.time.LocalDate
import scala.concurrent.Future


trait PaymentService {

  def initiatePayment(
                       payment: Payment,
                       callbackUrl: String
                     ): Future[Either[DomainError,PaymentGatewayResponse]]

  def handleWebhook(
                     payload: String,
                     signature: String
                   ): Future[Either[String, Payment]]

  def getPaymentById(ref:String):Future[Option[Payment]]
  def getPaymentStatus(status:PaymentStatus): Future[Seq[Payment]]
  def getPaymentByMethod(method: PaymentMethod): Future[Seq[Payment]]

  def getAllPayment:Future[Seq[Payment]]
  def deletePayment(ref:String): Future[Either[String, Unit]]

  def getDailyRevenue(date:LocalDate):Future[BigDecimal]
  def getWeeklyRevenue(date:LocalDate):Future[BigDecimal]
  def getMonthlyRevenue(year: Int, month: Int):Future[BigDecimal]

}
