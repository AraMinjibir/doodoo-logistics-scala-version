package infrastructure.persistence.models

import domain.models.ShipmentStatus

import java.time.Instant
import java.util.UUID

final case class ShipmentRow(
                              id: UUID,
                              trackingNumber: Option[String],
                              senderName: String,
                              recipientName: String,
                              recipientAddress: String,
                              recipientContact: String,
                              weight: Double,
                              length: Double,
                              width: Double,
                              height: Double,
                              contents: String,
                              status: ShipmentStatus,
                              estimatedDeliveryDate: Option[Instant],
                              createdAt: Instant,
                              cost: BigDecimal,
                              history: Option[String] // store tracking history as JSON string (optional)
                            )
