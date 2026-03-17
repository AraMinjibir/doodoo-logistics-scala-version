package controllers.dto

import domain.errors.DomainError
import domain.models._
import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

private[controllers] final case class DimensionsDto(
                                length: BigDecimal,
                                width: BigDecimal,
                                height: BigDecimal
                              )
private[controllers] object DimensionsDto {
  implicit val format: OFormat[DimensionsDto] = Json.format[DimensionsDto]
}

case class AddressDto(
                       street: String,
                       city: String,
                       state: String,
                       country: String,
                       postalCode: String
                     )

private[controllers] object AddressDto {
  def fromDomain(address: Address): AddressDto =
    AddressDto(
      street = address.street,
      city = address.city,
      state = address.state,
      country = address.country,
      postalCode = address.postalCode
    )
  implicit val format: OFormat[AddressDto] = Json.format[AddressDto]
}
private[controllers] final case class PackageDetailsDto(
                                    weight: BigDecimal,
                                    dimensions: DimensionsDto,
                                    contents: String
                                  )
private[controllers] object PackageDetailsDto {
  implicit val format: OFormat[PackageDetailsDto] = Json.format[PackageDetailsDto]
}

private[controllers] final case class RecipientDto(
                               name: String,
                               address: AddressDto,
                               contact: String
                             )
private[controllers] object RecipientDto {
  implicit val format: OFormat[RecipientDto] = Json.format[RecipientDto]
}

private[controllers] final case class CreateShipmentDto(
                                    senderName: String,
                                    streetName: String,
                                    streetNumber: String,
                                    city: String,
                                    state: String,
                                    country: String,
                                    postalCode: String,
                                    contact: String,
                                    weight:Double,
                                    length:Double,
                                    width:Double,
                                    height:Double,
                                    contents: String

                                  ) {
 def toShipment : Shipment = {
    val packageDetails = PackageDetails(
      weightInKilograms = this.weight,
      dimensions = Dimensions(
        lengthInCentimeters = this.length,
        widthInCentimeters  = this.width,
        heightInCentimeters = this.height
      ),
      contents = this.contents
    )
    val  recipient = Recipient(
      name = this.streetName,
      address = Address(
        street = this.streetNumber,
        city = this.city,
        state = this.state,
        country = this.country,
        postalCode = this.postalCode
      ),
      contact = this.contact
    )

    Shipment(
      senderName = this.senderName,
      recipient = recipient,
      packageDetails = packageDetails,
      serviceProviderId = None
    )
  }





}
private[controllers] final case class ProofOfDeliveryDto(
                                     image:Option[String],
                                     note:String,
                                     submittedBy: String,
                                     submittedAt:Option[Instant] = None
                                   )
private[controllers] object ProofOfDeliveryDto {
  implicit val format: OFormat[ProofOfDeliveryDto] = Json.format[ProofOfDeliveryDto]

  def toProofOfDeliveryDomain(dto:ProofOfDeliveryDto): Either[List[DomainError],ProofOfDelivery] = {
    ProofOfDelivery.createProofOfDelivery(
      image = dto.image,
      note = dto.note,
      submittedBy = dto.submittedBy,
      submittedAt = dto.submittedAt.getOrElse(Instant.now())
    )
  }


}
private[controllers] object CreateShipmentDto {
  implicit val format: OFormat[CreateShipmentDto] = Json.format[CreateShipmentDto]



  def toDto(domain: Shipment): ShipmentResponseDto = {
    ShipmentResponseDto(
      id = domain.id,
      trackingNumber = domain.trackingNumber,
      senderName = domain.senderName,
      recipient = toRecipientDto(domain.recipient),
      packageDetails =toPackageDetailsDto(domain.packageDetails),
      status = domain.status,
      estimatedDeliveryDate = domain.deliveryDateEstimate,
      createdAt = domain.createdAt,
      cost = domain.cost,
      proofOfDelivery = domain.proofOfDelivery,
      serviceProviderId = domain.serviceProviderId
    )
  }
  def toRecipientDto(domainRecipient: domain.models.Recipient): RecipientDto = {
    RecipientDto(
      name = domainRecipient.name,
      address = AddressDto.fromDomain(domainRecipient.address),
      contact = domainRecipient.contact
    )
  }
  def toDimensionsDto(domainDimensions: domain.models.Dimensions): DimensionsDto = {
    DimensionsDto(
      length = domainDimensions.lengthInCentimeters,
      width = domainDimensions.widthInCentimeters,
      height = domainDimensions.heightInCentimeters
    )
  }
  def toPackageDetailsDto(domainPackage: domain.models.PackageDetails): PackageDetailsDto = {
    PackageDetailsDto(
      weight = domainPackage.weightInKilograms,
      dimensions = toDimensionsDto(domainPackage.dimensions),
      contents = domainPackage.contents
    )
  }
  def toDomain(dto: CreateShipmentDto): Shipment = {
    val packageDetails = PackageDetails(
      weightInKilograms = dto.weight,
      dimensions = Dimensions(
        lengthInCentimeters = dto.length,
        widthInCentimeters  = dto.width,
        heightInCentimeters = dto.height
      ),
      contents = dto.contents
    )
    val  recipient = Recipient(
      name = dto.streetName,
      address = Address(
        street = dto.streetNumber,
        city = dto.city,
        state = dto.state,
        country = dto.country,
        postalCode = dto.postalCode
      ),
      contact = dto.contact
    )

    Shipment(
      senderName = dto.senderName,
      recipient = recipient,
      packageDetails = packageDetails,
      serviceProviderId = None
    )
  }

}


