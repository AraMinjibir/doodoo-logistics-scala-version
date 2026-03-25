package scala.repository

import domain.models.PaymentStatus
import infrastructure.persistence.tables.{PaymentTable, ShipmentsTable, UserTable}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.contain.allOf
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.{PaymentRepository, ShipmentRepository, UserRepository}
import slick.jdbc.JdbcProfile

import java.time.{LocalDate, ZoneId}
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt
import scala.helpers.{PaymentTestHelper, ShipmentTestHelpers}
import scala.util.Success

class SlickPaymentRepositoryIT extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ShipmentTestHelpers
  with PaymentTestHelper{

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

  lazy val repo     = app.injector.instanceOf[PaymentRepository]
  lazy val shipmentRepo     = app.injector.instanceOf[ShipmentRepository]
  lazy val userRepo = app.injector.instanceOf[UserRepository]
  lazy val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]
  import dbConfig.profile.api._

  override def beforeAll(): Unit = {
    val setupAction = DBIO.seq(
      PaymentTable.table.schema.dropIfExists,
      ShipmentsTable.table.schema.dropIfExists,
      UserTable.table.schema.dropIfExists,


      UserTable.table.schema.create,
      ShipmentsTable.table.schema.create,
      PaymentTable.table.schema.create
    )
    Await.result(dbConfig.db.run(setupAction), 5.seconds)
  }

  override def beforeEach(): Unit = {
    Await.result(
      dbConfig.db.run(
        DBIO.seq(
          PaymentTable.table.delete,
          ShipmentsTable.table.delete,
          UserTable.table.delete
        )), 5.seconds)
  }

  val user = newUser
  val shipment = createTestShipment()
  val payment =  newPayment(shipmentId = shipment.id,customerId = user.id)



  "SlickPaymentRepository" should {
    "Persist a payment row into the database" in {

      val newUser = Await.result(userRepo.createUser(user), 5.second)
      newUser shouldBe Success(1)

      val shipmentResult = Await.result(shipmentRepo.create(shipment), 5.second)
      shipmentResult shouldBe Success(1)

      val result = Await.result(repo.savePayment(payment), 5.second)
      result.isSuccess shouldBe true

      val row = Await.result(dbConfig.db.run(PaymentTable.table.result), 5.second)

      row should have size 1
      row.head.status shouldBe payment.status
      row.head.shipmentId shouldBe shipment.id
      row.head.referenceNumber shouldBe payment.referenceNumber

    }
    "Successfully update Payment status to Pending" in{
      val paymentStatus = PaymentStatus.Pending

      Await.result(userRepo.createUser(user), 5.second)

      Await.result(shipmentRepo.create(shipment), 5.second)

      Await.result(repo.savePayment(payment), 5.second)

      val updatedPayment = payment.copy(
        customerId = payment.customerId,
        shipmentId = payment.shipmentId,
        amount = payment.amount,
        status = paymentStatus,
        paidAt = payment.paidAt,
        paymentMethod = payment.paymentMethod,
        referenceNumber = payment.referenceNumber
      )

      val result = Await.result(repo.updatePayment(updatedPayment), 5.second)
      result shouldBe Success(1)

      val row = Await.result(dbConfig.db.run(PaymentTable.table.filter(_.referenceNumber === payment.referenceNumber).result.head), 5.second)

      row.status shouldBe paymentStatus
    }
    "Return a payment for a valid reference" in {
       Await.result(userRepo.createUser(user), 5.second)

      Await.result(shipmentRepo.create(shipment), 5.second)

      Await.result(repo.savePayment(payment),5.second)

      val result = Await.result(repo.getPaymentById(payment.referenceNumber), 5.second)
      result.map(_.referenceNumber) shouldBe Some(payment.referenceNumber)
    }
    "Retrieve a payment by status" in {

      Await.result(userRepo.createUser(user), 5.second)

      Await.result(shipmentRepo.create(shipment), 5.second)

      Await.result(repo.savePayment(payment), 5.second)

      val updatedStatus = payment.copy(status = PaymentStatus.Successful)

      Await.result(repo.updatePayment(updatedStatus), 5.second)

      val result = Await.result(repo.getPaymentStatus(PaymentStatus.Successful), 5.second)
      result should have size 1
      result.head.status shouldBe PaymentStatus.Successful
    }
    "Return empty when no payment found" in {
     val result = Await.result(repo.getAllPayment, 5.second)
      result shouldBe Seq.empty
    }
    "Return all payment inserted" in {
      Await.result(userRepo.createUser(user), 5.second)

      Await.result(shipmentRepo.create(shipment), 5.second)

      val firstPayment = newPayment(shipmentId = shipment.id,customerId = user.id)
      val secondPayment = newPayment(shipmentId = shipment.id,customerId = user.id)

      Await.result(repo.savePayment(firstPayment), 5.second)
      Await.result(repo.savePayment(secondPayment), 5.second)

      val result = Await.result(repo.getAllPayment, 5.second)

      result should have size 2
      result.map(_.referenceNumber) should contain allOf(firstPayment.referenceNumber, secondPayment.referenceNumber)
    }
    "Return the sum of daily payment inserted" in{

      val testDate = LocalDate.of(2026, 3, 4)

      Await.result(userRepo.createUser(user), 5.second)

      Await.result(shipmentRepo.create(shipment), 5.second)

      Await.result(repo.savePayment(payment), 5.second)

      val dailyRevenue = Await.result(repo.getDailyRevenue(testDate), 5.second)

      dailyRevenue mustBe BigDecimal(10000)
    }
    "Return the sum of weekly payment inserted" in {

      val startOfAWeek = LocalDate.of(2026, 3, 4)

      Await.result(userRepo.createUser(user), 5.second)
      Await.result(shipmentRepo.create(shipment), 5.second)
      Await.result(repo.savePayment(payment), 5.second)

      val weeklyRevenue = Await.result(repo.getWeeklyRevenue(startOfAWeek), 5.second)

      weeklyRevenue mustBe BigDecimal(10000)

    }
    "Return the sum of monthly payment inserted" in {

      val year = 2026
      val month = 3
      val zone = ZoneId.systemDefault()

      val aprilDate = LocalDate.of(year, month, 15)
      val aprilInstant = aprilDate.atStartOfDay(zone).toInstant

      val samplePayment = payment.copy(
        amount = BigDecimal(10000),
        paidAt = aprilInstant
      )

      Await.result(userRepo.createUser(user), 5.second)
      Await.result(shipmentRepo.create(shipment), 5.seconds)
      Await.result(repo.savePayment(payment), 5.seconds)

      val monthlyRevenue =
        Await.result(repo.getMonthlyRevenue(year, month), 5.seconds)

      monthlyRevenue mustBe BigDecimal(10000)
    }
    "Successfully remove the payment inserted" in {

      Await.result(userRepo.createUser(user), 5.second)
      Await.result(shipmentRepo.create(shipment), 5.second)

      Await.result(repo.savePayment(payment), 5.second)

      val result =  Await.result(repo.deletePayment(payment.referenceNumber), 5.second)

      result shouldBe Success(1)

      val row =  Await.result(dbConfig.db.run(PaymentTable.table.result), 5.second)

      row shouldBe empty

    }
    "return 0 when deleting a non-existent payment" in {
      val result = Await.result(repo.deletePayment("rf-doodood"), 5.second)

      result shouldBe Success(0)
    }
  }

}
