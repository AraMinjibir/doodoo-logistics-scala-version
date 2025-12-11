package api.dto

import domain.models.{Address, ShipmentStatus}
import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

final case class DimensionsDto(
                                length: BigDecimal,
                                width: BigDecimal,
                                height: BigDecimal
                              )
object DimensionsDto { implicit val format = Json.format[DimensionsDto] }

final case class PackageDetailsDto(
                                    weight: BigDecimal,
                                    dimensions: DimensionsDto,
                                    contents: String
                                  )
object PackageDetailsDto { implicit val format = Json.format[PackageDetailsDto] }

final case class RecipientDto(
                               name: String,
                               address: Address,
                               contact: String
                             )
object RecipientDto { implicit val format = Json.format[RecipientDto] }

final case class TrackingEventDto(
                                   status: String,
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
