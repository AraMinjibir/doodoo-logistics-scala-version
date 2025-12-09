package mappers

import api.dto.{CreateShipmentDto, ShipmentDto}
import domain.models._
import infrastructure.persistence.models.ShipmentRow

import java.time.Instant
import java.util.UUID

object ShipmentMapper {


  // DTO → DOMAIN

  def toDomain(dto: CreateShipmentDto): Shipment = {
    val now = Instant.now()

    Shipment(
      id = UUID.randomUUID(),
      trackingNumber = None,
      senderName = dto.senderName,
      recipient = Recipient(
        name = dto.recipientName,
        address = dto.recipientAddress,
        contact = dto.recipientContact
      ),
      packageDetails = PackageDetails(
        weight = dto.weight,
        dimensions = Dimensions(dto.length, dto.width, dto.height),
        contents = dto.contents
      ),
      status = ShipmentStatus.Created,
      estimatedDeliveryDate = None,
      createdAt = now,
      cost = BigDecimal(0),  // If you have cost in DTO, map it here
      history = Seq(
        TrackingEvent(
          timestamp = now,
          status = ShipmentStatus.Created,
          location = Some(dto.recipientAddress)
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
      weight = domain.packageDetails.weight,
      length = domain.packageDetails.dimensions.length,
      width = domain.packageDetails.dimensions.width,
      height = domain.packageDetails.dimensions.height,
      contents = domain.packageDetails.contents,
      status = domain.status,
      estimatedDeliveryDate = domain.estimatedDeliveryDate,
      createdAt = domain.createdAt,
      cost = domain.cost,
      history =
        if (domain.history.isEmpty)
          None
        else Some(play.api.libs.json.Json.toJson(domain.history).toString())
    )
  }


  // ROW → DOMAIN

  def toDomain(row: ShipmentRow): Shipment = {

    val history: Seq[TrackingEvent] =
      row.history
        .map(jsonStr => play.api.libs.json.Json.parse(jsonStr).as[Seq[TrackingEvent]])
        .getOrElse(Seq.empty)

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
        dimensions = Dimensions(row.length, row.width, row.height),
        contents = row.contents
      ),
      status = row.status,
      estimatedDeliveryDate = row.estimatedDeliveryDate,
      createdAt = row.createdAt,
      cost = row.cost,
      history = history
    )
  }


  // DOMAIN → DTO

  def toDto(domain: Shipment): ShipmentDto = {
    ShipmentDto(
      id = domain.id,
      trackingNumber = domain.trackingNumber,
      senderName = domain.senderName,
      recipientName = domain.recipient.name,
      recipientAddress = domain.recipient.address,
      recipientContact = domain.recipient.contact,
      weight = domain.packageDetails.weight,
      length = domain.packageDetails.dimensions.length,
      width = domain.packageDetails.dimensions.width,
      height = domain.packageDetails.dimensions.height,
      contents = domain.packageDetails.contents,
      status = domain.status.toString,
      estimatedDeliveryDate = domain.estimatedDeliveryDate,
      createdAt = domain.createdAt,
      cost = domain.cost
    )
  }
}
