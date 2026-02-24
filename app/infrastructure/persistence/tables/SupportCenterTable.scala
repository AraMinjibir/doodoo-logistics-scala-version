package infrastructure.persistence.tables

import domain.models.{Comment, ComplaintStatus}
import infrastructure.persistence.models.SupportCenterRow
import play.api.libs.json.{JsValue, Json, OFormat}
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}
import slick.model.ForeignKeyAction.Cascade

import java.time.Instant
import java.util.UUID

class SupportCenterTable(tag: Tag) extends Table[SupportCenterRow](tag, "support_center"){
  import SupportCenterTable._

  def id = column[UUID]("id",O.PrimaryKey)
  def userId = column[UUID]("userId")
  def shipmentId = column[UUID]("shipmentId")
  def subject = column[String]("subject")
  def descrription = column[String]("description")
  def status = column[ComplaintStatus]("status")
  def createdAt = column[Instant]("created_at")
  def resolvedAt = column[Option[Instant]]("resolved_at")
  def resolvedBy = column[Option[UUID]]("resolved_by")
  def comment = column[Seq[Comment]]("comment")

  def supportFk = foreignKey("shipment_foreign_key", shipmentId, ShipmentsTable.table)(_.id, onDelete = Cascade)

  def * :ProvenShape[SupportCenterRow] = (
    id,
    userId,
    shipmentId,
    subject,
    descrription,
    status,
    createdAt,
    resolvedAt,
    resolvedBy,
    comment
  ) <> ((SupportCenterRow.apply _).tupled, SupportCenterRow.unapply)
}

object SupportCenterTable {

  val table = TableQuery[SupportCenterTable]

  implicit val commentFormat: OFormat[Comment] = Json.format[Comment]

  implicit val commentColumnType: BaseColumnType[Seq[Comment]] =
    MappedColumnType.base[Seq[Comment], String](
      seq => Json.stringify(Json.toJson(seq)),
      str =>
        if (str == null || str.isEmpty) Seq.empty
        else Json.parse(str).as[Seq[Comment]]
    )


  // Enum mapping for ComplaintStatus
  implicit val complainStatusColumnType: BaseColumnType[ComplaintStatus] =
    MappedColumnType.base[ComplaintStatus, String](
      _.toString,
      str =>
        ComplaintStatus
          .fromString(str)
          .getOrElse(
            throw new IllegalArgumentException(s"Unknown ComplaintStatus: $str")
          )
    )

}