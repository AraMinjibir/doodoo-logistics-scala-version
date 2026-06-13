package scala.repository.read

import domain.models.ShipmentStatus
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import infrastructure.persistence.tables.ShipmentsTable
import repositories.SlickShipmentRepository

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.helpers.ShipmentTestHelpers
import play.api.Application
import slick.dbio.DBIO

class SlickShipmentReadRepositoryIT extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ShipmentTestHelpers {

  implicit val ec: ExecutionContext = ExecutionContext.global

  override def fakeApplication() =
    GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:test_read_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> ""
      ).build()

  // Repositories injected via app injector
  lazy val repo = app.injector.instanceOf[SlickShipmentRepository]
  lazy val dbConfig  = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]

  import dbConfig.profile.api._

  override def beforeAll(): Unit = {
  val reset = DBIO.seq(
    // DROP (child → parent)
    SupportCenterTable.table.schema.dropIfExists,
    ShipmentsTable.table.schema.dropIfExists,
    UserTable.table.schema.dropIfExists,

    // CREATE (parent → child)
    UserTable.table.schema.create,
    ShipmentsTable.table.schema.create,
    SupportCenterTable.table.schema.create
  )

  Await.result(dbConfig.db.run(reset), 10.seconds)
}

  override def beforeEach(): Unit = {
  val cleanup = DBIO.seq(
    SupportCenterTable.table.delete,
    ShipmentsTable.table.delete,
    UserTable.table.delete
  )

  Await.result(dbConfig.db.run(cleanup), 5.seconds)
}

  "SlickShipmentReadRepository" should {

    "findByTrackingNumber" should {
      "return Some(Shipment) when tracking number exists" in {
        val shipment = createTestShipment(tracking = "FIND-ME")

        Await.result(repo.create(shipment), 5.seconds)
        val result = Await.result(repo.findByTrackingNumber("FIND-ME"), 5.seconds)

        result.map(_.id) shouldBe Some(shipment.id)
      }

      "return None when tracking number does not exist" in {
        val result = Await.result(repo.findByTrackingNumber("GHOST-123"), 5.seconds)
        result shouldBe None
      }
    }

    "getByStatus" should {
      "return only shipments matching the status" in {
        val inTransit = createTestShipment(
          id = UUID.randomUUID(),
          tracking = "T1",
          status = ShipmentStatus.InTransit
        )
        val delivered = createTestShipment(
          id = UUID.randomUUID(),
          tracking = "T2",
          status = ShipmentStatus.Delivered
        )

        Await.result(repo.create(inTransit), 5.seconds)
        Await.result(repo.create(delivered), 5.seconds)

        val result = Await.result(repo.getByStatus(ShipmentStatus.InTransit), 5.seconds)

        result should have size 1
        result.head.trackingNumber shouldBe Some("T1")
      }
    }

    "getById" should {
      "return the shipment for a valid UUID" in {
        val shipment = createTestShipment()
        Await.result(repo.create(shipment), 5.seconds)

        val result = Await.result(repo.getById(shipment.id), 5.seconds)
        result.map(_.id) shouldBe Some(shipment.id)
      }
    }

    "listAll" should {
      "return empty list when no data exists" in {
        val result = Await.result(repo.listAll(0, 10), 5.seconds)
        result shouldBe empty
      }
    }
  }
}
