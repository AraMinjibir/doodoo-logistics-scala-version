package infrastructure.persistence.models

import domain.models.{PaymentMethod, PaymentStatus}

import java.time.Instant
import java.util.UUID

case class PaymentRow(
                       customerId: UUID,
                       shipmentId:UUID,
                       amount:BigDecimal,
                       status: PaymentStatus,
                       paidAt: Instant,
                       paymentMethod: PaymentMethod,
                       referenceNumber:String
                     )
