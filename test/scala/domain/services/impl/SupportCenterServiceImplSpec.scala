package scala.domain.services.impl

import domain.models.{Comment, Complaint, ComplaintStatus}
import domain.models.ComplaintStatus.InProgress
import domain.services.impl.SupportCenterServiceImpl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.{BeforeAndAfterEach, stats}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import repositories.SupportCenterRepository

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.helpers.SupportCenterTestHelper
import scala.util.{Failure, Success}

class SupportCenterServiceImplSpec
  extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with SupportCenterTestHelper{

  val mockRepo = mock[SupportCenterRepository]
  val service =  new SupportCenterServiceImpl(mockRepo)
  override def beforeEach(): Unit = {
    reset(mockRepo)
  }
val complaint= complaintPass

  "SupportCenterServiceImpl" should {
    "CREATE a complaint successfully" in {
      when(mockRepo.createComplaint(any()))
        .thenReturn(Future.successful(Success(1)))

      val result = Await.result(service.createComplaint(complaint), 5.seconds)

      result.isRight shouldBe true
      verify(mockRepo).createComplaint(any[Complaint])
    }
//    "RETURN error when repository insert fails" in {
//      when(mockRepo.createComplaint(any()))
//        .thenReturn(Future.successful(Failure(new RuntimeException("DB error"))))
//
//      val result = Await.result(service.createComplaint(complaint), 5.second)
//
//      result.isLeft shouldBe true
//    }

    "GET complaint by id" in {
      when(mockRepo.getComplaintById(complaintId))
        .thenReturn(Future.successful(Some(complaint)))

      val result = Await.result(service.getComplaintById(complaintId), 5.second)
      result shouldBe Some(complaint)
    }
    "RETURN None when complaint does not exist with the given id" in {
      when(mockRepo.getComplaintById(any()))
        .thenReturn(Future.successful(None))

      val result = Await.result(service.getComplaintById(complaintId), 5.second)
      result shouldBe None
    }

    "GET complaint by status" in {
      when(mockRepo.getComplaintByStatus(status))
        .thenReturn(Future.successful(Seq(complaint)))

      val result = Await.result(service.getComplaintByStatus(status), 5.second)
      result shouldBe Seq(complaint)
    }
    "RETURN None when complaint does not exist with the status given" in {
      when(mockRepo.getComplaintByStatus(any()))
        .thenReturn(Future.successful(Seq.empty))

      val result = Await.result(service.getComplaintByStatus(InProgress), 5.second)
      result shouldBe Seq.empty
    }

    "Return a sequence of all complaints" in {
      when(mockRepo.getAllComplaint)
        .thenReturn(Future.successful(Seq(complaint)))

      val result = Await.result(service.getAllComplaint, 5.second)
      result shouldBe Seq(complaint)
    }
    "Return an empty sequence when complaint is found" in{
      when(mockRepo.getAllComplaint)
        .thenReturn(Future.successful(Seq.empty))

      val result = Await.result(service.getAllComplaint, 5.second)
      result shouldBe Seq.empty
    }

    "Update complaint status to In progress" in {

      when(mockRepo.getComplaintById(any[UUID]))
        .thenReturn(Future.successful(Some(complaint)))

      when(mockRepo.updateComplaintStatus(any[Complaint]))
        .thenReturn(Future.successful(Success(1)))

      val result =
        Await.result(service.markComplaintAsInProgress(complaintId), 5.seconds)

      result.isRight shouldBe true
    }

  "Update complaint status to Resolved" in {

    val complaintInProgress =
      complaint.copy(status = ComplaintStatus.InProgress)

    when(mockRepo.getComplaintById(any[UUID]))
      .thenReturn(Future.successful(Some(complaintInProgress)))

    when(mockRepo.updateComplaintStatus(any[Complaint]))
      .thenReturn(Future.successful(Success(1)))

    val result =
      Await.result(service.markComplaintAsResolved(complaintId,agentId), 5.seconds)

    result.isRight shouldBe true
  }

    "Add new comment successfully" in {

      val newComment = Comment(
        id = id,
        complaintId = complaintId,
        authorId = authorId,
        message = "Package received",
        createdAt = Instant.now()
      )

      when(mockRepo.addComment(any[UUID], any[Comment]))
        .thenReturn(Future.successful(Success(1)))

      val result =
        Await.result(service.addComment(complaintId, newComment), 5.seconds)

      result.isRight shouldBe true
      result.toOption.get.message shouldBe "Package received"
    }

}
}
