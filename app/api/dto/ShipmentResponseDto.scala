package api.dto

import domain.models.ShipmentStatus
import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

final case class TrackingEventDto(
                                   status: ShipmentStatus,
                                   timestamp: Instant,
                                   location: Option[String]
                                 )
object TrackingEventDto { implicit val format = Json.format[TrackingEventDto] }

final case class ShipmentResponseDto(
                              id: UUID,
                              trackingNumber: Option[String],
                              senderName: String,
                              recipient: RecipientDto,
                              packageDetails: PackageDetailsDto,
                              status: ShipmentStatus,
                              estimatedDeliveryDate: Option[Instant],
                              createdAt: Instant,
                              cost: BigDecimal,
                              history: Seq[TrackingEventDto]
                            )

object ShipmentResponseDto {
  implicit val format: OFormat[ShipmentResponseDto] = Json.format[ShipmentResponseDto]
}
