package api.dto

import play.api.libs.json.{Json, OFormat}
import java.time.Instant
import java.util.UUID

case class ShipmentDto(
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
                        status: String,
                        estimatedDeliveryDate: Option[Instant],
                        createdAt: Instant,
                        cost: BigDecimal
                      )

object ShipmentDto {
  implicit val format: OFormat[ShipmentDto] = Json.format[ShipmentDto]
}
