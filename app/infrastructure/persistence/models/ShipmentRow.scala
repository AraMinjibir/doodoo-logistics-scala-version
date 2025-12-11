package infrastructure.persistence.models

import domain.models.{Address, ShipmentStatus}

import java.time.Instant
import java.util.UUID

final case class ShipmentRow(
                              id: UUID,
                              trackingNumber: Option[String],
                              senderName: String,
                              recipientName: String,
                              recipientAddress: Address,
                              recipientContact: String,
                              weight: BigDecimal,
                              length: BigDecimal,
                              width: BigDecimal,
                              height: BigDecimal,
                              contents: String,
                              status: ShipmentStatus,
                              estimatedDeliveryDate: Option[Instant],
                              createdAt: Instant,
                              cost: BigDecimal,
                              history: String
                            )
