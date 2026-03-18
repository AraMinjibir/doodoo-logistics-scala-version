package scala.domain.helpers

import domain.models.{Complaint, ComplaintStatus}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.OK
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSClient

import java.time.Instant
import java.util.UUID
import scala.collection.immutable.{List, Seq}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait SupportCenterTestHelper {
  val id = UUID.randomUUID()
  val status = ComplaintStatus.Open
  val createdAt = Instant.now()
  val resolvedAt = Some(Instant.parse("2026-03-09T18:36:27Z"))
  val resolvedBy = Some(UUID.fromString("11111111-1111-1111-1111-111111111111"))
  val comment = Seq.empty
  val complaintStatus = ComplaintStatus.InProgress
  val resolvedComplaintStatus = ComplaintStatus.Resolved
  val authorId = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val complaintId = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val agentId = UUID.fromString("11111111-1111-1111-1111-111111111111")



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
        createdAt = Instant.now(), resolvedAt = Some(Instant.now()),
        resolvedBy = resolvedBy, comment = Seq.empty),
      errors
    )
  }
  def newComplaint(
                  userId:UUID,
                shipmentId: UUID,
                subject: String = "Complaint",
                description: String = "Package lost"):Complaint =
   createComplaint(
    userId = userId,
    shipmentId = shipmentId ,
    subject = subject,
    description = description,
  ).fold(
      errors => throw new RuntimeException(errors.mkString(",")),
      identity
    )


  val complaintPass: Complaint = newComplaint(shipmentId = id, userId = id)

  val validUserPayload: JsObject = Json.obj(
    "name" -> "Test User",
    "email" -> "test@example.com",
    "password" -> "hashed_pw",
    "phone" -> "1234567890",
    "role" -> "Admin"
  )


}
