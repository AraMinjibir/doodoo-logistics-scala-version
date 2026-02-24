package domain.models

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
  def markInProgress(): Complaint =
    if (status == ComplaintStatus.Open)
      copy(status = ComplaintStatus.InProgress)
    else
      throw new IllegalStateException("Only open complaints can move to in progress")

  def resolve(agentId: UUID, now: Instant): Complaint =
    if (status != ComplaintStatus.Resolved)
      copy(
        status = ComplaintStatus.Resolved,
        resolvedAt = Some(now),
        resolvedBy = Some(agentId)
      )
    else
      throw new IllegalStateException("Complaint already resolved")

}



object Complaint {

  def createComplaint(
              userId: UUID,
              shipmentId: UUID,
              subject: String,
              description: String,
            ): Either[List[Throwable],Complaint] = {
    val errors: List[IllegalArgumentException] = List(
      Option.when(userId.toString.trim.isEmpty)(s"User id must be provided: $userId"),
      Option.when(shipmentId.toString.trim.isEmpty)(s"Shipment id must  be provided: $shipmentId"),
      Option.when(subject.trim.isEmpty)(s"Subject must be provided: $subject"),
      Option.when(description.trim.isEmpty)(s"Description must not to be empty: $description")
    )
      .flatten.map(new IllegalArgumentException(_))
    Either.cond(
      errors.isEmpty,
      Complaint(id = UUID.randomUUID() ,  userId = userId , shipmentId = shipmentId , subject = subject ,
        description = description, status = ComplaintStatus.Open,
        createdAt = Instant.now(), resolvedAt = None, resolvedBy = None, comment = Seq.empty),
      errors
    )
  }
}
object Comment {
  def createComment(id: UUID,
                    complaintId: UUID,
                    authorId: UUID,
                    message: String,
                    internal: Boolean,
                    ):Either[List[Throwable],Comment] = {
    val errors: List[IllegalArgumentException] = List(
      Option.when(complaintId.toString.trim.isEmpty)(s"Complain id must not be empty: $complaintId"),
      Option.when(authorId.toString.trim.isEmpty)(s"author id must be provided: $authorId"),
      Option.when(message.trim.isEmpty)(s"message must be provided: $message")
    )
      .flatten
      .map(new IllegalArgumentException(_))
      Either.cond(
        errors.isEmpty,
        Comment(id, complaintId, authorId, message, internal, createdAt = Instant.now()),
        errors
      )

  }
}

