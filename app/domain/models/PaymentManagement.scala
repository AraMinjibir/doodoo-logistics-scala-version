package domain.models

import java.time.Instant
import java.util.UUID

case class Payment private(
                                      customerId: UUID,
                                      shipmentId:UUID,
                                      amount:BigDecimal,
                                      status: PaymentStatus,
                                      paidAt: Instant,
                                      paymentMethod: PaymentMethod,
                                      referenceNumber:String,
                                      gatewayTransactionId: Option[String],
                                      failureReason: Option[String]
                  )

object Payment {

  def generateReferenceNumber(prefix: String = "RF-DODO"): String = {
    val id = UUID.randomUUID().toString.replace("-", "").take(10).toUpperCase
    s"$prefix-$id"
  }

  def generatePayment(customerId: UUID,shipmentId:UUID,amount:BigDecimal, paymentMethod:PaymentMethod) = {
    val errors: List[String] = List(
      Option.when(customerId.toString.trim.isEmpty)(s"CustomerId must be provided: $customerId"),
      Option.when(shipmentId.toString.trim.isEmpty)(s"Shipment id must be provided: $shipmentId"),
      Option.when(amount <= 0)(s"Amount can't be negative: $amount"),
      Option.when(
        !Set[PaymentMethod](
          PaymentMethod.Card,
          PaymentMethod.MobileMoney,
          PaymentMethod.BankTransfer
        ).contains(paymentMethod)
      )(s"Invalid payment method: $paymentMethod")
    ).flatten
    Either.cond(
      errors.isEmpty,
      Payment(
        customerId = customerId,
        shipmentId = shipmentId,
        amount = amount,
        status = PaymentStatus.Pending,
        paidAt  = Instant.now(),
        paymentMethod = paymentMethod,
        referenceNumber =  Payment.generateReferenceNumber(),
        gatewayTransactionId = None,
        failureReason = None
    ),
      errors)
  }

  def canTransition(
                     from: PaymentStatus,
                     to: PaymentStatus
                   ): Boolean = {

    (from, to) match {

      case (PaymentStatus.Pending, PaymentStatus.Successful) => true
      case (PaymentStatus.Pending, PaymentStatus.Failed)     => true

      case _ => false
    }
  }


}
