package mappers

import api.dto.{CreateShipmentDto, DimensionsDto, PackageDetailsDto, RecipientDto, ShipmentResponseDto, TrackingEventDto}
import domain.models._
import infrastructure.persistence.models.ShipmentRow
import com.google.inject.Singleton

import java.time.Instant
import java.util.UUID
import play.api.libs.json.Json

@Singleton
class ShipmentRowMapper {
  // DOMAIN → ROW
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
object ShipmentMapper {

  // DTO → DOMAIN
  def toDomain(dto: CreateShipmentDto): Shipment = {
    val now = Instant.now()
    val locationString: String =
      s"${dto.recipient.address.street}, ${dto.recipient.address.city}, ${dto.recipient.address.state}"

    Shipment(
      id = UUID.randomUUID(),
      trackingNumber = None,
      senderName = dto.senderName,
      recipient = Recipient(
        name = dto.recipient.name,
        address = dto.recipient.address,
        contact = dto.recipient.contact
      ),
      packageDetails = PackageDetails(
        weight = dto.packageDetails.weight,
        dimensions = Dimensions(
          length = dto.packageDetails.dimensions.length,
          width = dto.packageDetails.dimensions.width,
          height = dto.packageDetails.dimensions.height
        ),
        contents = dto.packageDetails.contents
      ),
      status = ShipmentStatus.Created,
      estimatedDeliveryDate = None,
      createdAt = now,
      updatedAt = now,
      cost = BigDecimal(0),
      history = Seq(
        TrackingEvent(
          status = ShipmentStatus.Created,
          timestamp = now,
          location = Some(locationString)
        )
      )
    )
  }

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

  // DOMAIN → RESPONSE DTO

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
      address = domainRecipient.address,
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

  def toTrackingEventDto(domainEvent: domain.models.TrackingEvent): TrackingEventDto = {
    TrackingEventDto(
      status = domainEvent.status,
      timestamp = domainEvent.timestamp,
      location = domainEvent.location
    )
  }
}
