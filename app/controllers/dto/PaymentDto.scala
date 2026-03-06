package controllers.dto

import domain.models.{Payment, PaymentMethod, PaymentStatus}
import play.api.libs.json.{Format, Json, OFormat}

import java.time.Instant
import java.util.UUID

private[controllers] case class PaymentRequestDto(
                                                   customerId: UUID,
                                                   shipmentId:UUID,
                                                   amount:BigDecimal,
                                                   paymentMethod:PaymentMethod
                                                 ) {
  def toPaymentDomain:Either[List[String],Payment] =
    Payment.generatePayment(
      customerId = this.customerId,
      shipmentId = this.shipmentId,
      amount = this.amount,
      paymentMethod = this.paymentMethod
    )
}

private [controllers] case class PaymentResponseDto(
                                                     referenceNumber:String,
                                                     customerId: UUID,
                                                     shipmentId:UUID,
                                                     amount:BigDecimal,
                                                     status: PaymentStatus,
                                                     paidAt: Instant,
                                                     paymentMethod: PaymentMethod,
                                                     gatewayTransactionId: Option[String],
                                                     failureReason: Option[String]
                                                   )

object PaymentRequestDto {
  implicit val format:OFormat[PaymentRequestDto] = Json.format[PaymentRequestDto]

  def toPaymentDto(domain:Payment):PaymentRequestDto = PaymentRequestDto(
    customerId = domain.customerId,
    shipmentId = domain.shipmentId,
    amount = domain.amount,
    paymentMethod = domain.paymentMethod
  )
}

object PaymentResponseDto {
  implicit val paymentStatusFormat: Format[PaymentStatus] =
    controllers.json.PaymentStatusJson.format

  implicit val paymentMethodFormat: Format[PaymentMethod] =
    controllers.json.PaymentMethodJson.format
  implicit val format:OFormat[PaymentResponseDto] = Json.format[PaymentResponseDto]

  def toPaymentResponseDto(domain:Payment): PaymentResponseDto = {
    PaymentResponseDto(
      referenceNumber = domain.referenceNumber,
      customerId = domain.customerId,
      shipmentId = domain.shipmentId,
      amount = domain.amount,
      status = domain.status,
      paidAt = domain.paidAt,
      paymentMethod = domain.paymentMethod,
      gatewayTransactionId = domain.gatewayTransactionId,
      failureReason = domain.failureReason
    )
  }
}
