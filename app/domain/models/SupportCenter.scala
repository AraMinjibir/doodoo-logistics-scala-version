package domain.models

import domain.errors.InvalidComplaintState

import java.time.Instant
import java.util.UUID


case class Comment private(
                          id: UUID,
                         complaintId: UUID,
                         authorId: UUID,
                         message: String,
                        internal: Boolean,
                         createdAt : Instant
                           )


case class Complaint private(
                          id: UUID,
                          userId: UUID,
                          shipmentId: UUID,
                          subject: String,
                          description: String,
                          status: ComplaintStatus,
                          createdAt:  Instant,
                          resolvedAt: Option[Instant],
                          resolvedBy: Option[UUID],
                          comment: Seq[Comment]
                        ) {
  def markInProgress(): Either[(InvalidComplaintState), Complaint] =
    status match {
      case ComplaintStatus.Open =>
        Right(copy(status = ComplaintStatus.InProgress))

      case current =>
        Left(InvalidComplaintState(current, ComplaintStatus.InProgress))
    }

  def resolve(agentId: UUID, now: Instant):  Either[(InvalidComplaintState), Complaint] =
    status match {
      case ComplaintStatus.Open | ComplaintStatus.InProgress =>
        Right(copy(
          status = ComplaintStatus.Resolved,
          resolvedAt = Some(now),
          resolvedBy = Some(agentId)
        ))
      case current =>
        Left(InvalidComplaintState(current, ComplaintStatus.Resolved))
    }

}



object Complaint {

  def createComplaint(
              userId: UUID,
              shipmentId: UUID,
              subject: String,
              description: String,
            ): Either[List[String],Complaint] = {
    val errors: List[String] = List(
      Option.when(userId.toString.trim.isEmpty)(s"User id must be provided: $userId"),
      Option.when(shipmentId.toString.trim.isEmpty)(s"Shipment id must be provided: $shipmentId"),
      Option.when(subject.trim.isEmpty)(s"Subject must be provided: $subject"),
      Option.when(description.trim.isEmpty)(s"Description must not be empty: $description")
    ).flatten
    Either.cond(
      errors.isEmpty,
      Complaint(id = UUID.randomUUID() ,  userId = userId , shipmentId = shipmentId , subject = subject ,
        description = description, status = ComplaintStatus.Open,
        createdAt = Instant.now(), resolvedAt = None, resolvedBy = None, comment = Seq.empty),
      errors
    )
  }

  def validComplaint(c:Complaint): Either[List[String],Complaint] = {
    Complaint.createComplaint(
      userId = c.userId ,
      shipmentId = c.shipmentId ,
      subject = c.subject ,
      description = c.description
    )
  }
}
object Comment {
  def createComment(id: UUID,
                    complaintId: UUID,
                    authorId: UUID,
                    message: String,
                    internal: Boolean,
                    ):Either[List[String],Comment] = {
    val errors: List[String] = List(
      Option.when(complaintId.toString.trim.isEmpty)(s"Complain id must not be empty: $complaintId"),
      Option.when(authorId.toString.trim.isEmpty)(s"author id must be provided: $authorId"),
      Option.when(message.trim.isEmpty)(s"message must be provided: $message")
    )
      .flatten
      Either.cond(
        errors.isEmpty,
        Comment(id = UUID.randomUUID(), complaintId = complaintId,
          authorId = authorId, message = message, internal = internal, createdAt = Instant.now()),
        errors
      )

  }

  def validComment(c:Comment): Either[List[String],Comment] = {
    Comment.createComment(
      id = c.id,
      complaintId = c.complaintId,
      authorId = c.authorId,
      message = c.message,
      internal = c.internal
    )
  }
}

