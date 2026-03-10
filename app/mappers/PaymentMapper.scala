package mappers

import com.google.inject.Singleton
import domain.models.Payment
import infrastructure.persistence.models.PaymentRow

@Singleton
class PaymentMapper {
  def fromDomain(domain:Payment):PaymentRow = {
    PaymentRow(
      customerId = domain.customerId,
      shipmentId = domain.shipmentId,
      amount = domain.amount,
      status = domain.status,
      paidAt = domain.paidAt,
      paymentMethod = domain.paymentMethod,
      referenceNumber = domain.referenceNumber,
      gatewayTransactionId = domain.gatewayTransactionId, failureReason = domain.failureReason
    )
  }
  def fromRow(row:PaymentRow):Payment = {
    Payment(
      customerId = row.customerId,
      shipmentId = row.shipmentId,
      amount = row.amount,
      status = row.status,
      paidAt = row.paidAt,
      paymentMethod = row.paymentMethod,
      referenceNumber = row.referenceNumber,
      gatewayTransactionId = row.gatewayTransactionId,
      failureReason = row.failureReason
    )
  }

}
