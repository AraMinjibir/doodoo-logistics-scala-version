package infrastructure.persistence.models

import domain.models.{Address, Dimensions, PackageDetails, Recipient, Shipment, ShipmentStatus, TrackingEvent}
import play.api.libs.json.Json

import java.time.Instant
import java.util.UUID

final case class ShipmentRow(
                              id: UUID,
                              trackingNumber: Option[String],
                              senderName: String,
                              recipientName: String,
                              recipientAddress: Address,
                              recipientContact: String,
                              weight: BigDecimal,
                              length: BigDecimal,
                              width: BigDecimal,
                              height: BigDecimal,
                              contents: String,
                              status: ShipmentStatus,
                              estimatedDeliveryDate: Option[Instant],
                              createdAt: Instant,
                              updatedAt: Instant,
                              cost: BigDecimal,
                              history: String
                            ) {
  // ROW → DOMAIN

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
        weight = row.weight,
        dimensions = Dimensions(
          length = row.length,
          width = row.width,
          height = row.height
        ),
        contents = row.contents
      ),
      status = row.status,
      estimatedDeliveryDate = row.estimatedDeliveryDate,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt,
      cost = row.cost,
      history = Json.parse(row.history).as[Seq[TrackingEvent]]

    )
  }
  def toRow(domain: Shipment): ShipmentRow = {
    ShipmentRow(
      id = domain.id,
      trackingNumber = domain.trackingNumber,
      senderName = domain.senderName,
      recipientName = domain.recipient.name,
      recipientAddress = domain.recipient.address,
      recipientContact = domain.recipient.contact,
      weight = domain.packageDetails.weight.toDouble,
      length = domain.packageDetails.dimensions.length.toDouble,
      width = domain.packageDetails.dimensions.width.toDouble,
      height = domain.packageDetails.dimensions.height.toDouble,
      contents = domain.packageDetails.contents,
      status = domain.status,
      estimatedDeliveryDate = domain.estimatedDeliveryDate,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      cost = domain.cost,
      history = Json.toJson(domain.history).toString()
    )
  }

}
