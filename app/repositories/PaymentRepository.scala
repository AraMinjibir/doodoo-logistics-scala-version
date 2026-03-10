package repositories

import domain.models.{Payment, PaymentMethod, PaymentStatus}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

trait PaymentRepository {

  def makePayment(payment:Payment): Future[Try[Int]]
  def getPaymentById(ref:String):Future[Option[Payment]]
  def getPaymentStatus(status:PaymentStatus): Future[Seq[Payment]]
  def getPaymentByMethod(method: PaymentMethod): Future[Seq[Payment]]
  def getPaymentByShipmentId(shipmentId: UUID): Future[Option[Payment]]

  def getAllPayment:Future[Seq[Payment]]
  def updatePayment(payment: Payment):Future[Try[Int]]
  def deletePayment(ref:String): Future[Try[Int]]

  def getDailyRevenue(date:LocalDate):Future[BigDecimal]
  def getWeeklyRevenue(date:LocalDate):Future[BigDecimal]
  def getMonthlyRevenue(year: Int, month: Int):Future[BigDecimal]



}
