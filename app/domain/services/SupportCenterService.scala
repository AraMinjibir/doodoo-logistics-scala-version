package domain.services

import domain.errors.DomainError
import domain.models.{Comment, Complaint, ComplaintStatus}

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future

trait SupportCenterService {

  def createComplaint(complaint: Complaint): Future[Either[DomainError,Complaint]]
  def getComplaintById(complaintId:UUID): Future[Option[Complaint]]
  def getComplaintByStatus(status:ComplaintStatus): Future[Seq[Complaint]]
  def getAllComplaint:Future[Seq[Complaint]]
  def markComplaintAsInProgress( complaintId: UUID):Future[Either[DomainError, Complaint]]
  def markComplaintAsResolved(complaintId: UUID, agentId: UUID, now: Instant):Future[Either[DomainError, Complaint]]
  def addComment(newComment: Comment): Future[Either[DomainError, Comment]]

}
