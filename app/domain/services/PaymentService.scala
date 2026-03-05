package domain.services

import domain.models.{Payment, PaymentMethod, PaymentStatus}

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Try

trait PaymentService {

  def initiatePayment(
                       payment: Payment,
                       callbackUrl: String
                     ): Future[String]

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
