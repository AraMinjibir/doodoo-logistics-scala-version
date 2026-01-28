package controllers.dto

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
//                                  Address
                                    streetName: String,
                                    streetNumber: String,
                                    city: String,
                                    state: String,
                                    country: String,
                                    postalCode: String,
                                    contact: String,
//                             Dimension
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
      estimatedDeliveryDate = domain.estimatedDeliveryDate,
      createdAt = domain.createdAt,
      cost = domain.cost,
      history = domain.history.map(toTrackingEventDto)
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
      length = domainDimensions.length,
      width = domainDimensions.width,
      height = domainDimensions.height
    )
  }
  def toPackageDetailsDto(domainPackage: domain.models.PackageDetails): PackageDetailsDto = {
    PackageDetailsDto(
      weight = domainPackage.weight,
      dimensions = toDimensionsDto(domainPackage.dimensions),
      contents = domainPackage.contents
    )
  }
  def toTrackingEventDto(domainEvent: TrackingEvent): TrackingEventDto = {
    TrackingEventDto(
      status = domainEvent.status,
      timestamp = domainEvent.timestamp,
      location = domainEvent.location
    )
  }

}


