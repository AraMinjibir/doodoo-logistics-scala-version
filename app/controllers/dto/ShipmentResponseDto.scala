package controllers.dto

import domain.models.{ProofOfDelivery, Shipment, ShipmentStatus}
import play.api.libs.json.{Format, Json, OFormat}

import java.time.Instant
import java.util.UUID

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
                              proofOfDelivery: Seq[ProofOfDelivery] = Seq.empty,
                              serviceProviderId: Option[UUID]
                                    )

private[controllers] object ShipmentResponseDto {
  implicit val shipmentStatusleFormat: Format[ShipmentStatus] =
    controllers.json.ShipmentStatusJson.format
  implicit val proofFormat: OFormat[ProofOfDelivery] = Json.format[ProofOfDelivery]
  implicit val format: OFormat[ShipmentResponseDto] = Json.format[ShipmentResponseDto]


  def fromDomain(domain:Shipment): ShipmentResponseDto = {
    val  recipientDto:RecipientDto = RecipientDto(
      name = domain.recipient.name,
      contact = domain.recipient.contact,
      address = AddressDto(
        street = domain.recipient.address.street,
        city = domain.recipient.address.city,
        state = domain.recipient.address.state,
        country = domain.recipient.address.country,
        postalCode = domain.recipient.address.postalCode
      )
    )
    val dimensionDto:DimensionsDto = DimensionsDto(
      length = domain.packageDetails.dimensions.lengthInCentimeters,
      width = domain.packageDetails.dimensions.widthInCentimeters,
      height = domain.packageDetails.dimensions.heightInCentimeters
    )
    val packageDetailsDto: PackageDetailsDto = PackageDetailsDto(
      weight = domain.packageDetails.weightInKilograms,
      dimensions = dimensionDto,
      contents = domain.packageDetails.contents
    )
  ShipmentResponseDto(
    id = domain.id,
    trackingNumber = domain.trackingNumber,
    senderName = domain.senderName,
    recipient = recipientDto,
    packageDetails = packageDetailsDto,
    status = domain.status,
    estimatedDeliveryDate = domain.deliveryDateEstimate,
    createdAt = domain.createdAt,
    cost = domain.cost,
    proofOfDelivery = domain.proofOfDelivery,
    serviceProviderId = domain.serviceProviderId
  )
  }
}




