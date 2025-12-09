package infrastructure.persistence.tables

import infrastructure.persistence.models.ShipmentRow
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import play.api.libs.json.Json
import domain.models.{ShipmentStatus, TrackingEvent}

import java.time.Instant
import java.util.UUID

class ShipmentsTable(tag: Tag) extends Table[ShipmentRow](tag, "shipments") {

  //  MappedColumnType for serialize history  JSON.
  implicit val trackingEventFormat = Json.format[TrackingEvent]
  implicit val seqTrackingEventColumnType =
    MappedColumnType.base[Seq[TrackingEvent], String](
      events => Json.toJson(events).toString(),
      str => if (str == null || str.isEmpty) Seq.empty else Json.parse(str).as[Seq[TrackingEvent]]
    )

  implicit val shipmentStatusColumnType =
    MappedColumnType.base[ShipmentStatus, String](
      status => status.toString,
      str => ShipmentStatus.fromString(str)
    )

  def id = column[UUID]("id", O.PrimaryKey)
  def trackingNumber = column[Option[String]]("tracking_number")
  def senderName = column[String]("sender_name")
  def recipientName = column[String]("recipient_name")
  def recipientAddress = column[String]("recipient_address")
  def recipientContact = column[String]("recipient_contact")
  def weight = column[Double]("weight")
  def length = column[Double]("length")
  def width = column[Double]("width")
  def height = column[Double]("height")
  def contents = column[String]("contents")
  def status = column[ShipmentStatus]("status")
  def estimatedDeliveryDate = column[Option[Instant]]("estimated_delivery_date")
  def createdAt = column[Instant]("created_at")
  def cost = column[BigDecimal]("cost")
  def history = column[Option[String]]("history") // persisted as JSON string

  def * = (
    id,
    trackingNumber,
    senderName,
    recipientName,
    recipientAddress,
    recipientContact,
    weight,
    length,
    width,
    height,
    contents,
    status,
    estimatedDeliveryDate,
    createdAt,
    cost,
    history
  ) <> (
    (ShipmentRow.apply _).tupled,
    ShipmentRow.unapply
  )
}

object ShipmentsTable {
  val table = TableQuery[ShipmentsTable]
}
