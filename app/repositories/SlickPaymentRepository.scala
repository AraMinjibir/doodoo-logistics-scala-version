package repositories

import com.google.inject.{Inject, Singleton}
import domain.models.{Payment, PaymentMethod, PaymentStatus}
import infrastructure.persistence.tables.PaymentTable
import mappers.PaymentMapper
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import infrastructure.persistence.tables.PaymentTable._

import java.time.{Instant, LocalDate, ZoneId}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SlickPaymentRepository @Inject()(
                                        dbConfigProvider: DatabaseConfigProvider,
                                        mapper:PaymentMapper
                                      )(implicit ex: ExecutionContext) extends PaymentRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val db = dbConfig.db
  import dbConfig.profile.api._
  private val q = PaymentTable.table
  private val zone = ZoneId.systemDefault()

  private def revenueBetween(start: Instant, end: Instant): Future[BigDecimal] = {
    val query = q
      .filter(p => p.paidAt >= start && p.paidAt < end)
      .map(_.amount)
      .sum
      .result

    db.run(query).map(_.getOrElse(BigDecimal(0)))
  }

  override def savePayment(payment: Payment): Future[Try[Int]] = {
    val row = mapper.fromDomain(payment)
    val insert = q += row
    db.run(insert.asTry)
  }

  override def getPaymentById(ref: String): Future[Option[Payment]] =
    db.run(q.filter(_.referenceNumber === ref).result.headOption).map(_.map(mapper.fromRow))

  override def getPaymentStatus(status: PaymentStatus): Future[Seq[Payment]] =
    db.run(q.filter(_.status === status).result).map(_.map(mapper.fromRow))

  override def getPaymentByMethod(method: PaymentMethod): Future[Seq[Payment]] =
    db.run(q.filter(_.paymentMethod === method).result).map(_.map(mapper.fromRow))

  override def getAllPayment: Future[Seq[Payment]] = db.run(q.result).map(_.map(mapper.fromRow))

  override def updatePayment(payment: Payment): Future[Try[Int]] = {
    val row = mapper.fromDomain(payment)
    val updated = q.filter(_.referenceNumber === payment.referenceNumber).update(row)
    db.run(updated.asTry)
  }

  override def getDailyRevenue(date: LocalDate): Future[BigDecimal] = {
    val start = date.atStartOfDay(zone).toInstant
    val end   = date.plusDays(1).atStartOfDay(zone).toInstant
    revenueBetween(start, end)
  }

  override def getWeeklyRevenue(startOfWeek: LocalDate): Future[BigDecimal] = {
    val start = startOfWeek.atStartOfDay(zone).toInstant
    val end = startOfWeek.plusWeeks(1).atStartOfDay(zone).toInstant

    revenueBetween(start, end)
  }

  override def getMonthlyRevenue(year: Int, month: Int): Future[BigDecimal] = {

    val startDate = LocalDate.of(year, month, 1)
    val start = startDate.atStartOfDay(zone).toInstant
    val end = startDate.plusMonths(1).atStartOfDay(zone).toInstant

    revenueBetween(start, end)
  }

  override def deletePayment(ref: String): Future[Try[Int]] = {
    val delete = q.filter(_.referenceNumber === ref).delete
    db.run(delete.asTry)
  }

 override def getPaymentByShipmentId(shipmentId: UUID): Future[Option[Payment]] =
    db.run(
      PaymentTable.table
        .filter(_.shipmentId === shipmentId)
        .result
        .headOption
    ).map(_.map(mapper.fromRow))
}
