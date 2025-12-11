package api.dto

import domain.models.Address
import play.api.libs.json.{Json, OFormat}

final case class DimensionsDto(
                                length: BigDecimal,
                                width: BigDecimal,
                                height: BigDecimal
                              )
object DimensionsDto {
  implicit val format: OFormat[DimensionsDto] = Json.format[DimensionsDto]
}

final case class PackageDetailsDto(
                                    weight: BigDecimal,
                                    dimensions: DimensionsDto,
                                    contents: String
                                  )
object PackageDetailsDto {
  implicit val format: OFormat[PackageDetailsDto] = Json.format[PackageDetailsDto]
}

final case class RecipientDto(
                               name: String,
                               address: Address,
                               contact: String
                             )
object RecipientDto {
  implicit val format: OFormat[RecipientDto] = Json.format[RecipientDto]
}

final case class CreateShipmentDto(
                                    senderName: String,
                                    recipient: RecipientDto,
                                    packageDetails: PackageDetailsDto
                                  )
object CreateShipmentDto {
  implicit val format: OFormat[CreateShipmentDto] = Json.format[CreateShipmentDto]
}
