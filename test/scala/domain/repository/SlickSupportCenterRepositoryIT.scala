package scala.domain.repository

import domain.models.UserRole.Admin
import domain.models.{Comment, ComplaintStatus, User}
import repositories.{ShipmentRepository, SupportCenterRepository, UserRepository}
import infrastructure.persistence.tables.{ShipmentsTable, SupportCenterTable, UserTable}

import scala.util.Success
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, stats}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.domain.helpers.{ShipmentTestHelpers, SupportCenterTestHelper}

 class SlickSupportCenterRepositoryIT extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with SupportCenterTestHelper with ShipmentTestHelpers{

   implicit val ec: ExecutionContext = ExecutionContext.global

   override def fakeApplication() =
     GuiceApplicationBuilder()
       .configure(
         "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
         "slick.dbs.default.db.driver" -> "org.h2.Driver",
         "slick.dbs.default.db.url" -> "jdbc:h2:mem:test_write_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
         "slick.dbs.default.db.user" -> "sa",
         "slick.dbs.default.db.password" -> ""
       ).build()

   lazy val repo     = app.injector.instanceOf[SupportCenterRepository]
   lazy val shipmentRepo     = app.injector.instanceOf[ShipmentRepository]
   lazy  val userRepo =  app.injector.instanceOf[UserRepository]
   lazy val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]
   import dbConfig.profile.api._

   override def beforeAll(): Unit = {
     val setupAction = DBIO.seq(
       SupportCenterTable.table.schema.dropIfExists,
       ShipmentsTable.table.schema.dropIfExists,
       UserTable.table.schema.dropIfExists,

       UserTable.table.schema.create,
       ShipmentsTable.table.schema.create,
       SupportCenterTable.table.schema.create
     )
     Await.result(dbConfig.db.run(setupAction), 5.seconds)
   }

   override def beforeEach(): Unit = {
     Await.result(
       dbConfig.db.run(
         DBIO.seq(
         SupportCenterTable.table.delete,
         ShipmentsTable.table.delete,
           UserTable.table.delete
       )), 5.seconds)
   }

   val shipment = createTestShipment()

   val newUser = User.createUser(
     name = "DooDoo User",
     email = "DooDooUser@gmail.com",
     password = "doodoooaauiiq1234",
     phone = "07022223456",
     role = Admin
   ).fold(
     errors => throw new RuntimeException(errors.mkString(",")),
     identity
   )

   val complaint = newComplaint(shipmentId = shipment.id, userId = newUser.id)

    "SlickSupportCenterRepository" should {
      "Persist a complaint row into the database" in {

        val user = Await.result(userRepo.createUser(newUser), 5.second)
        user shouldBe Success(1)

        val shipmentResult = Await.result(shipmentRepo.create(shipment), 5.second)
        shipmentResult shouldBe Success(1)

        val result = Await.result(repo.createComplaint(complaint), 5.second)
        result.isSuccess shouldBe true
        result.get shouldBe 1
        val row = Await.result(dbConfig.db.run(SupportCenterTable.table.result), 5.second)

        row should have size 1
        row.head.shipmentId shouldBe complaint.shipmentId


      }
      "Successfully update complaint status to In progress" in {
        val user = Await.result(userRepo.createUser(newUser), 5.second)
        user shouldBe Success(1)

        val shipment = createTestShipment()
        Await.result(shipmentRepo.create(shipment), 5.seconds)

        Await.result(repo.createComplaint(complaint), 5.second)

        val updated = complaint.copy(
          id = complaint.id,
          userId = complaint.userId,
          shipmentId = shipmentId,
          subject = complaint.subject,
          description = complaint.description,
          status = complaintStatus,
          createdAt = createdAt,
          resolvedAt = resolvedAt,
          resolvedBy = resolvedBy,
          comment = comment
        )

        val result = Await.result(repo.updateComplaintStatus(updated),5.second)
        result shouldBe Success(1)

        val persisted = Await.result(dbConfig.db.run(SupportCenterTable.table.filter(_.id === complaint.id).result.head),5.second)
        persisted.status shouldBe ComplaintStatus.InProgress
      }
      "Successfully update complaint status to Resolved" in {

        val user = Await.result(userRepo.createUser(newUser), 5.second)
        user shouldBe Success(1)

        val shipment = createTestShipment()
        Await.result(shipmentRepo.create(shipment), 5.seconds)

        Await.result(repo.createComplaint(complaint), 5.second)

        val updated = complaint.copy(
          id = complaint.id,
          userId = complaint.userId,
          shipmentId = shipmentId,
          subject = complaint.subject,
          description = complaint.description,
          status = resolvedComplaintStatus,
          createdAt = createdAt,
          resolvedAt = resolvedAt,
          resolvedBy = resolvedBy,
          comment = comment
        )

        val result = Await.result(repo.updateComplaintStatus(updated),5.second)
        result shouldBe Success(1)

        val persisted = Await.result(dbConfig.db.run(SupportCenterTable.table.filter(_.id === complaint.id).result),5.second)
        val saved = persisted.head

        saved.status shouldBe ComplaintStatus.Resolved
        saved.resolvedAt.isDefined shouldBe true
        saved.resolvedBy shouldBe complaint.resolvedBy
      }
      "Return a complaint for a valid id" in {

        val user = Await.result(userRepo.createUser(newUser), 5.second)
        user shouldBe Success(1)

        Await.result(shipmentRepo.create(shipment),5.second)
        Await.result(repo.createComplaint(complaint), 5.second)

        val result = Await.result(repo.getComplaintById(complaint.id), 5.second)
        result.map(_.id) shouldBe Some(complaint.id)

      }
      "should retrieve complaints by status" in {

        val user = Await.result(userRepo.createUser(newUser), 5.second)
        user shouldBe Success(1)

        val shipment = createTestShipment()
        Await.result(shipmentRepo.create(shipment), 5.seconds)

        val complaint = newComplaint(shipmentId = shipment.id, userId = newUser.id)
        Await.result(repo.createComplaint(complaint), 5.seconds)

        val updated = complaint.copy(status = ComplaintStatus.InProgress)
        Await.result(repo.updateComplaintStatus(updated), 5.seconds)

        val result =
          Await.result(
            repo.getComplaintByStatus(ComplaintStatus.InProgress),
            5.seconds
          )

        result should have size 1
        result.head.status shouldBe ComplaintStatus.InProgress
      }
      "should return empty when no complaints exist" in {

        val result =
          Await.result(repo.getAllComplaint, 5.seconds)

        result shouldBe empty
      }
      "should return all inserted complaints" in {

        val user = Await.result(userRepo.createUser(newUser), 5.second)
        user shouldBe Success(1)

        val shipment = createTestShipment()
        Await.result(shipmentRepo.create(shipment), 5.seconds)

        val complaint1 = newComplaint(shipmentId = shipment.id, userId = newUser.id)
        val complaint2 = newComplaint(shipmentId = shipment.id, userId = newUser.id)

        Await.result(repo.createComplaint(complaint1), 5.seconds)
        Await.result(repo.createComplaint(complaint2), 5.seconds)

        val result =
          Await.result(repo.getAllComplaint, 5.seconds)

        result should have size 2
        result.map(_.id) should contain allOf (complaint1.id, complaint2.id)
      }
      "should successfully add a comment" in {

        val user = Await.result(userRepo.createUser(newUser), 5.second)
        user shouldBe Success(1)

        val shipment = createTestShipment()
        Await.result(shipmentRepo.create(shipment), 5.seconds)

        val complaint = newComplaint(shipmentId = shipment.id, userId = newUser.id)
        Await.result(repo.createComplaint(complaint), 5.seconds)

        val comment = Comment(
          id = id,
          authorId = authorId ,
          complaintId = complaint.id,
          message = "Test comment",
          createdAt = createdAt
        )

        val result =
          Await.result(
            repo.addComment(complaint.id, comment),
            5.seconds
          )

        result.isSuccess shouldBe true
        result.get shouldBe 1
      }
    }

}
