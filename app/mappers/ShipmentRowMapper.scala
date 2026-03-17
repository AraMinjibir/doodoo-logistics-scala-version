package mappers

import com.google.inject.Singleton
import domain.models.{Dimensions, PackageDetails, Recipient, Shipment, ShipmentStatus, ProofOfDelivery}
import infrastructure.persistence.models.ShipmentRow
import play.api.libs.json.{Format, Json, OFormat}

@Singleton
class ShipmentRowMapper {

  implicit val shipmentStatusleFormat: Format[ShipmentStatus] =
    controllers.json.ShipmentStatusJson.format
  implicit val trackingEventFormat: OFormat[ProofOfDelivery] = Json.format[ProofOfDelivery]

  // DOMAIN → ROW
  def toRow(domain: Shipment): ShipmentRow = {
    ShipmentRow(
      id = domain.id,
      trackingNumber = domain.trackingNumber,
      senderName = domain.senderName,
      recipientName = domain.recipient.name,
      recipientStreet = domain.recipient.address.street,
      recipientCity = domain.recipient.address.city,
      recipientState = domain.recipient.address.state,
      recipientCountry = domain.recipient.address.country,
      recipientPostalCode = domain.recipient.address.postalCode,
      recipientContact = domain.recipient.contact,
      weight = domain.packageDetails.weightInKilograms,
      length = domain.packageDetails.dimensions.lengthInCentimeters,
      width = domain.packageDetails.dimensions.widthInCentimeters,
      height = domain.packageDetails.dimensions.heightInCentimeters,
      contents = domain.packageDetails.contents,
      status = domain.status,
      estimatedDeliveryDate = domain.deliveryDateEstimate,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      cost = domain.cost,
      proofOfDelivery = domain.proofOfDelivery,
      serviceProviderId = domain.serviceProviderId
    )
  }
  def fromRow(row: ShipmentRow): Shipment = {

    Shipment(
      id = row.id,
      trackingNumber = row.trackingNumber,
      senderName = row.senderName,
      recipient = Recipient(
        name = row.recipientName,
        address = row.recipientAddress,
        contact = row.recipientContact
      ),
      packageDetails = PackageDetails(
        weightInKilograms = row.weight,
        dimensions = Dimensions(
        widthInCentimeters = row.width,
          lengthInCentimeters = row.length,
          heightInCentimeters = row.height
        ),
        contents = row.contents
      ),
      status = row.status,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      proofOfDelivery = row.proofOfDelivery,
      serviceProviderId = row.serviceProviderId
    )
  }


}


