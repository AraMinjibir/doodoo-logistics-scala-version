package controllers.dto

import domain.models.{Comment, Complaint, ComplaintStatus}
import play.api.libs.json.{Format, Json, OFormat}

import java.time.Instant
import java.util.UUID

private[controllers] final case class CommentResponseDto(
                                                         id: UUID,
                                                         complaintId: UUID,
                                                         authorId: UUID,
                                                         message: String,
                                                         createdAt : Instant
                                                       )

private[controllers] case class ComplaintResponseDto(
                                                     id: UUID,
                                                     userId: UUID,
                                                     shipmentId: UUID,
                                                     subject: String,
                                                     description: String,
                                                     status: ComplaintStatus,
                                                     createdAt:  Instant,
                                                     resolvedAt: Option[Instant],
                                                     resolvedBy: Option[UUID],
                                                     comment: Seq[CommentRequestDto]
                                                   )
object ComplaintResponseDto {
  implicit val complaintStatusleFormat: Format[ComplaintStatus] =
    controllers.json.ComplaintStatusJson.format
  implicit val format:OFormat[ComplaintResponseDto] = Json.format[ComplaintResponseDto]

  def toDto(domain:Complaint): ComplaintResponseDto = {
    ComplaintResponseDto(
      id = domain.id,
      userId = domain.userId,
      shipmentId = domain.shipmentId,
      subject = domain.subject,
      description = domain.description,
      status = domain.status, createdAt = domain.createdAt,
      resolvedAt = domain.resolvedAt,
      resolvedBy = domain.resolvedBy,
      comment = domain.comment.map(CommentRequestDto.fromCommentDomain)
    )
  }

}
object CommentResponseDto {
  implicit val format:OFormat[CommentResponseDto] = Json.format[CommentResponseDto]

  def toCommentDto(dto:Comment):CommentResponseDto ={
    CommentResponseDto(id = dto.id,
      complaintId = dto.complaintId,
      authorId = dto.authorId,
      message = dto.message,
      createdAt = dto.createdAt
    )
  }
}