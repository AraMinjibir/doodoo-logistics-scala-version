package infrastructure.persistence.tables

import domain.models.{PaymentMethod, PaymentStatus}
import infrastructure.persistence.models.PaymentRow
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.PostgresProfile.api._
import slick.model.ForeignKeyAction.Cascade

import java.time.Instant
import java.util.UUID

class PaymentTable(tag:Tag) extends Table[PaymentRow](tag,"payments") {

  import PaymentTable._

  def customerId = column[UUID]("customer_id")
  def shipmentId = column[UUID]("shipment_id")
  def amount = column[BigDecimal]("amount")
  def status = column[PaymentStatus]("status")
  def  paidAt = column[Instant]("paid_at")
  def paymentMethod = column[PaymentMethod]("payment_method")
  def referenceNumber = column[String]("reference_number", O.PrimaryKey)
  def gatewayTransactionId = column[Option[String]]("gateway_transaction_id")
  def failureReason = column [Option[String]]("failure_reason")

  def paymentFrk = foreignKey("shipment_foreign_key", shipmentId, ShipmentsTable.table)(_.id, onDelete = Cascade)

  def * :ProvenShape[PaymentRow] = (
    customerId,shipmentId,amount,status,paidAt,paymentMethod,referenceNumber,gatewayTransactionId,failureReason
  ) <> ((PaymentRow.apply _).tupled, PaymentRow.unapply)

}

object PaymentTable {

  val table = TableQuery[PaymentTable]

  implicit val paymentStatusColumnType: BaseColumnType[PaymentStatus] =
    MappedColumnType.base[PaymentStatus, String](
      _.toString,
      str =>
        PaymentStatus
          .fromString(str)
          .getOrElse(
            throw new IllegalArgumentException(s"Unknown PaymentStatus: $str")
          )
    )

  implicit val paymentMethodColumnType: BaseColumnType[PaymentMethod] =
    MappedColumnType.base[PaymentMethod, String](
      _.toString,
      str =>
        PaymentMethod
          .fromString(str)
          .getOrElse(
            throw new IllegalArgumentException(s"Unknown PaymentMethod: $str")
          )
    )
}
