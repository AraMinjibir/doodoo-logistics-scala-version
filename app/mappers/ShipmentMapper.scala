package mappers

import domain.models._
import infrastructure.persistence.models.ShipmentRow
import api.dto.{CreateShipmentDto, ShipmentDto}

import java.util.UUID
import java.time.Instant
import play.api.libs.json.Json

object ShipmentMapper {

  // Domain -> Row
  def toRow(domain: Shipment): ShipmentRow = {
    val histJson =
      if (domain.history.isEmpty) None
      else Some(Json.toJson(domain.history).toString())

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
      status = ShipmentStatus.Draft,     // sealed trait -> String
      estimatedDeliveryDate = domain.estimatedDeliveryDate,
      createdAt = domain.createdAt,
      cost = domain.cost,
      historyJson = histJson
    )
  }

  // Row -> Domain
  def toDomain(row: ShipmentRow): Shipment = {
    val history =
      row.historyJson
        .filter(_.nonEmpty)
        .flatMap(json => Json.parse(json).asOpt[Seq[TrackingEvent]])
        .getOrElse(Seq.empty)

    Shipment(
      id = row.id,
      trackingNumber = row.trackingNumber,
      senderName = row.senderName,
      recipient = Recipient(row.recipientName, row.recipientAddress, row.recipientContact),
      packageDetails = PackageDetails(
        row.weight,
        Dimensions(row.length, row.width, row.height),
        row.contents
      ),
      status = ShipmentStatus.Draft,
      estimatedDeliveryDate = row.estimatedDeliveryDate,
      createdAt = row.createdAt,
      cost = row.cost,
      history = history
    )
  }

  // CreateShipmentDto -> Domain
  def createDtoToDomain(dto: CreateShipmentDto): Shipment = {
    Shipment(
      id = UUID.randomUUID(),
      trackingNumber = None,
      senderName = dto.senderName,
      recipient = Recipient(dto.recipientName, dto.recipientAddress, dto.recipientContact),
      packageDetails = PackageDetails(
        dto.weight,
        Dimensions(dto.length, dto.width, dto.height),
        dto.contents
      ),
      status = ShipmentStatus.Draft,
      estimatedDeliveryDate = None,
      createdAt = Instant.now(),
      cost = BigDecimal(0),
      history = Seq.empty
    )
  }

  // Domain -> DTO
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
