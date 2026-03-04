package scala.domain.helpers

import domain.models.{Payment, PaymentMethod, PaymentStatus}

import java.time.Instant
import java.util.UUID
import scala.collection.immutable.{List, Set}
import scala.math.BigDecimal
import scala.util.Either

trait PaymentTestHelper {

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
        referenceNumber =  Payment.generateReferenceNumber()
      ),
      errors)
  }

  def newPayment(
                  customerId:UUID = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                  shipmentId:UUID,amount:BigDecimal = 10000,
                  paymentMethod:PaymentMethod = PaymentMethod.Card
                ):Payment = {
                  Payment.generatePayment(
                    customerId = customerId,
                    shipmentId = shipmentId,
                    amount = amount,
                    paymentMethod = paymentMethod
                  ).fold(
                    errors => throw new RuntimeException(errors.mkString(",")),
                    identity
                  )
  }
}
