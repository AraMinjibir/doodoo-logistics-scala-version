package controllers.dto

import domain.models.{Comment, Complaint, ComplaintStatus}
import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID


private[controllers] final case class CommentRequestDto(
                            complaintId: UUID,
                            authorId: UUID,
                            message: String,
                          ) {
  def toCommentDomain:Either[List[String],Comment] = {
    Comment.createComment(
      complaintId = this.complaintId,
      authorId = this.authorId,
      message = this.message,
    )
  }
}

private[controllers] case class ComplaintRequestDto(
                                                   userId: UUID,
                                                   shipmentId: UUID,
                                                   subject: String,
                                                   description: String
                                                 ) {
  def toComplaint: Either[List[String],Complaint] = {
    Complaint.createComplaint(
      userId = this.userId,
      shipmentId = this.shipmentId,
      subject = this.subject,
      description = this.description
    )
  }
}

object ComplaintRequestDto {
  implicit val format:OFormat[ComplaintRequestDto] = Json.format[ComplaintRequestDto]

  def fromDomain(domain:Complaint): ComplaintRequestDto = {
    ComplaintRequestDto(
      userId = domain.userId,
      shipmentId = domain.shipmentId,
      subject = domain.subject,
      description = domain.description
    )
  }
}
object CommentRequestDto {
  implicit val format:OFormat[ CommentRequestDto] = Json.format[ CommentRequestDto]
def fromCommentDomain(domain: Comment): CommentRequestDto = {
  CommentRequestDto(
    complaintId = domain.complaintId,
    authorId = domain.authorId,
    message = domain.message
  )
}
}