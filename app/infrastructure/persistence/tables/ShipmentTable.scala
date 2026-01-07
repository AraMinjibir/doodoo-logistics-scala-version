package infrastructure.persistence.tables

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import java.time.Instant
import java.util.UUID
import play.api.libs.json.{Format, Json, OFormat}
import domain.models.{Address, ShipmentStatus, TrackingEvent}
import infrastructure.persistence.models.ShipmentRow

class ShipmentsTable(tag: Tag) extends Table[ShipmentRow](tag, "shipments") {

  import ShipmentsTable._

  def id = column[UUID]("id", O.PrimaryKey)
  def trackingNumber = column[Option[String]]("tracking_number")
  def senderName = column[String]("sender_name")
  def recipientName = column[String]("recipient_name")
  def recipientStreet = column[String]("recipient_street")
  def recipientCity = column[String]("recipient_city")
  def recipientState = column[String]("recipient_state")
  def recipientCountry = column[String]("recipient_country")
  def recipientPostalCode = column[String]("recipient_postal_code")
  def recipientContact = column[String]("recipient_contact")
  def weight = column[BigDecimal]("weight")
  def length = column[BigDecimal]("length")
  def width = column[BigDecimal]("width")
  def height = column[BigDecimal]("height")
  def contents = column[String]("contents")
  def status = column[ShipmentStatus]("status")
  def estimatedDeliveryDate = column[Option[Instant]]("estimated_delivery_date")
  def updatedAt = column[Instant]("updated_at")
  def createdAt = column[Instant]("created_at")
  def cost = column[BigDecimal]("cost")
  def history = column[String]("history")


  def * =
    (
      id,
      trackingNumber,
      senderName,
      recipientName,
      (recipientStreet, recipientCity, recipientState, recipientCountry, recipientPostalCode).mapTo[Address],
      recipientContact,
      weight,
      length,
      width,
      height,
      contents,
      status,
      estimatedDeliveryDate,
      createdAt,updatedAt,
      cost,
      history
    ) <> ((ShipmentRow.apply _).tupled, ShipmentRow.unapply)
}

object ShipmentsTable {

  val table = TableQuery[ShipmentsTable]

  implicit val shipmentStatusleFormat: Format[ShipmentStatus] =
    controllers.json.ShipmentStatusJson.format

  implicit val trackingEventFormat: OFormat[TrackingEvent] = Json.format[TrackingEvent]

  // JSON → String mapping for history
  implicit val seqTrackingEventColumnType: BaseColumnType[Seq[TrackingEvent]] =
    MappedColumnType.base[Seq[TrackingEvent], String](
      seq => Json.toJson(seq).toString(),
      json =>
        if (json == null || json.isEmpty) Seq.empty
        else Json.parse(json).as[Seq[TrackingEvent]]
    )

  // Enum mapping for ShipmentStatus
  implicit val shipmentStatusColumnType: BaseColumnType[ShipmentStatus] =
    MappedColumnType.base[ShipmentStatus, String](
      _.toString,
      str =>
        ShipmentStatus
          .fromString(str)
          .getOrElse(
            throw new IllegalArgumentException(s"Unknown ShipmentStatus: $str")
          )
    )

  // Address JSON
  implicit val addressFormat: OFormat[Address] = Json.format[Address]

  implicit val addressColumnType: BaseColumnType[Address] =
    MappedColumnType.base[Address, String](
      addr => Json.toJson(addr).toString(),
      json => Json.parse(json).as[Address]
    )
}
