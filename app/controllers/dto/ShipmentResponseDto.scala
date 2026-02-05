package controllers.dto

import domain.models.ShipmentStatus
import play.api.libs.json.{Format, Json, OFormat}

import java.time.Instant
import java.util.UUID

private[controllers] final case class TrackingEventDto(
                                   status: ShipmentStatus,
                                   timestamp: Instant,
                                   location: Option[String]
                                 )
private[controllers] object TrackingEventDto {
  implicit val shipmentStatusleFormat: Format[ShipmentStatus] =
    controllers.json.ShipmentStatusJson.format
  implicit val format: OFormat[TrackingEventDto] = Json.format[TrackingEventDto] }

private [controllers]
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
                            )

private[controllers] object ShipmentResponseDto {
  implicit val shipmentStatusleFormat: Format[ShipmentStatus] =
    controllers.json.ShipmentStatusJson.format
  implicit val format: OFormat[ShipmentResponseDto] = Json.format[ShipmentResponseDto]
}


