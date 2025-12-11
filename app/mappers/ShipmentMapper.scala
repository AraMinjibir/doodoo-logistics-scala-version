package mappers

import api.dto.{CreateShipmentDto, RecipientDto, ShipmentResponseDto}
import domain.models.{ShipmentStatus, _}
import infrastructure.persistence.models.ShipmentRow
import api.dto.{DimensionsDto, PackageDetailsDto, TrackingEventDto}

import java.time.Instant
import java.util.UUID
import play.api.libs.json.Json
object ShipmentMapper {

  // DTO → DOMAIN
  def toDomain(dto: CreateShipmentDto): Shipment = {
    val now = Instant.now()
    val locationString: String = s"${dto.recipient.address.street}, ${dto.recipient.address.city}, ${dto.recipient.address.state}"
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
      cost = BigDecimal(0),  // Or use your cost calculation
      history = Seq(
        TrackingEvent(
          status = ShipmentStatus.Created,
          timestamp = now,
          location = Some(locationString)
        )
      )

    )
  }


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
      cost = domain.cost,
      history = domain.history.toString
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
      cost = row.cost,
      history = Json.parse(row.history).as[Seq[TrackingEvent]]    )
  }


  // DOMAIN → RESPONSE DTO ----------------------------------------------

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

  def toRecipientDto(domainRecipient: domain.models.Recipient): api.dto.RecipientDto = {
    RecipientDto(
      name = domainRecipient.name,
      address = domainRecipient.address,
      contact = domainRecipient.contact
    )
  }


  def toDimensionsDto(domainDimensions: domain.models.Dimensions): api.dto.DimensionsDto = {
    DimensionsDto(
      length = domainDimensions.length,
      width = domainDimensions.width,
      height = domainDimensions.height
    )
  }
  def toPackageDetailsDto(domainPackage: domain.models.PackageDetails): api.dto.PackageDetailsDto = {
    PackageDetailsDto(
      weight = domainPackage.weight,
      dimensions = toDimensionsDto(domainPackage.dimensions),
      contents = domainPackage.contents
    )
  }

  def toTrackingEventDto(domainEvent: domain.models.TrackingEvent): api.dto.TrackingEventDto = {
    TrackingEventDto(
      status = ShipmentStatus.toString(domainEvent.status),
      timestamp = domainEvent.timestamp,
      location = domainEvent.location
    )
  }
}
