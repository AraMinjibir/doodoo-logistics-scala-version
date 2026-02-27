package domain.services.impl

import com.google.inject.{Inject, Singleton}
import controllers.SupportCenterController
import controllers.helpers.ResultMapper
import domain.errors.{CommentValidation, ComplaintNotFound, ComplaintValidation, DomainError, InvalidComplaintState, ValidationError}
import domain.models.{Comment, Complaint, ComplaintStatus}
import domain.services.SupportCenterService
import play.api.Logger
import repositories.SupportCenterRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class SupportCenterServiceImpl @Inject()(
                                        repo:SupportCenterRepository
                                        )(implicit ec:ExecutionContext)
                                        extends SupportCenterService with ResultMapper {

  private val logger = Logger(classOf[SupportCenterController])


  override def createComplaint(complaint: Complaint): Future[Either[DomainError,Complaint]] = {
    Complaint.validComplaint(complaint) match {
      case Left(errors) =>
        Future.successful(Left(ComplaintValidation(errors)))

      case Right(validComplaint) =>
        repo.createComplaint(validComplaint).map{
          case Success(_) => Right(validComplaint)
          case Failure(ex) =>
            logger.error("Database failure", ex)
            Left(mapInsertException(ex))
      }
    }
  }
  override def getComplaintById(complaintId:UUID): Future[Option[Complaint]] = repo.getComplaintById(complaintId)
  override def getComplaintByStatus(status:ComplaintStatus): Future[Seq[Complaint]] = repo.getComplaintByStatus(status)
  override def getAllComplaint:Future[Seq[Complaint]] = repo.getAllComplaint
  override def markComplaintAsInProgress( complaintId: UUID):Future[Either[DomainError, Complaint]] = {
    repo.getComplaintById(complaintId).flatMap{
      case None => Future.successful(Left(ComplaintNotFound(complaintId.toString)))
      case Some(existingComplaint) => existingComplaint.markInProgress() match {
        case Left(InvalidComplaintState(from, to)) => Future.successful(Left(InvalidComplaintState(from, to)))
        case Right(updated) =>
          repo.updateComplaintStatus(updated).map {
            case Success(_) =>
              Right(updated)

            case Failure(ex) =>
              Left(mapInsertException(ex))
          }
      }
    }
  }
  override def markComplaintAsResolved(complaintId: UUID, agentId: UUID):Future[Either[DomainError, Complaint]] = {
    repo.getComplaintById(complaintId).flatMap{
      case None => Future.successful(Left(ComplaintNotFound(complaintId.toString)))
      case Some(definedComplaint) => definedComplaint.resolve(agentId) match {
        case Left(InvalidComplaintState(from, to)) => Future.successful(Left(InvalidComplaintState(from, to)))
        case Right(updatedComplaint) => repo.updateComplaintStatus(updatedComplaint).map{
          case Success(_) => Right(updatedComplaint)
          case Failure(ex) => Left(mapInsertException(ex))
        }
      }
    }
  }
  override def addComment(complaintId: UUID, newComment: Comment): Future[Either[DomainError, Comment]] = {

    Comment.validComment(newComment) match {
      case Left(err) => Future.successful(Left(CommentValidation(err)))
      case Right(validComment) => repo.addComment(validComment.complaintId, validComment).map{
        case Success(_) => Right(validComment)
        case Failure(ex) => Left(mapInsertException(ex))
      }
    }
  }

}
