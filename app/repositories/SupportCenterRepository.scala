package repositories

import domain.models.{Comment, Complaint, ComplaintStatus}

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

trait SupportCenterRepository {
  def createComplaint(complaint: Complaint): Future[Try[Int]]
  def getComplaintById(id:UUID): Future[Option[Complaint]]
  def getComplaintByStatus(status:ComplaintStatus): Future[Seq[Complaint]]
  def getAllComplaint:Future[Seq[Complaint]]
  def updateComplaintStatus(complaint: Complaint):Future[Try[Int]]
  def addComment(complaintId: UUID, newComment: Comment): Future[Try[Int]]
}
