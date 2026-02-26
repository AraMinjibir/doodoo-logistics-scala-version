package repositories

import com.google.inject.{Inject, Singleton}
import domain.models.{Comment, Complaint, ComplaintStatus}
import infrastructure.persistence.tables.SupportCenterTable
import mappers.SupportRowMapper
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SlickSupportCenterRepository @Inject()(
                                              dbConfigProvider: DatabaseConfigProvider,
                                              mapper:SupportRowMapper
                                            ) (implicit ec: ExecutionContext) extends SupportCenterRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig.profile.api._
  import infrastructure.persistence.tables.SupportCenterTable._
  private val db = dbConfig.db

  private val q = SupportCenterTable.table

  override def createComplaint(complaint: Complaint): Future[Try[Int]] = {
    val row = mapper.fromDomain(complaint)
    val insert = q += row
    db.run(insert.asTry)
  }

  override def getComplaintById(complaintId:UUID): Future[Option[Complaint]] = {
    db.run(q.filter(_.id === complaintId ).result.headOption).map(_.map(mapper.toDomain))
  }
  override def getComplaintByStatus(status:ComplaintStatus): Future[Seq[Complaint]] = {
    db.run(q.filter(_.status === status).result)
      .map(_.map(mapper.toDomain))
  }
  override def getAllComplaint:Future[Seq[Complaint]] = {
    db.run(q.result).map(_.map(mapper.toDomain))
  }

  override def updateComplaintStatus(complaint: Complaint):Future[Try[Int]] = {
    val row = mapper.fromDomain(complaint)
    val update = q.filter(_.id === complaint.id).update(row)

    db.run(update.asTry)

  }

 override def addComment(complaintId: UUID, newComment: Comment): Future[Try[Int]]  = {
   val action =
     q.filter(_.id === complaintId)
       .result
       .head
       .flatMap { existing =>
         q.filter(_.id === complaintId)
           .map(_.comment)
           .update(existing.comment :+ newComment)
       }

   db.run(action.transactionally.asTry)
  }


}
