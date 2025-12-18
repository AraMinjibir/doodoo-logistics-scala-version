package scala.domain.repository.read

import domain.models.ShipmentStatus
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.read.SlickShipmentReadRepository
import repositories.write.SlickShipmentWriteRepository
import slick.jdbc.JdbcProfile
import infrastructure.persistence.tables.ShipmentsTable

import java.util.UUID
import scala.domain.helpers.ShipmentTestHelpers
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

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
  lazy val readRepo  = app.injector.instanceOf[SlickShipmentReadRepository]
  lazy val writeRepo = app.injector.instanceOf[SlickShipmentWriteRepository]
  lazy val dbConfig  = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]

  import dbConfig.profile.api._

  override def beforeAll(): Unit = {
    val setupAction = DBIO.seq(
      ShipmentsTable.table.schema.dropIfExists,
      ShipmentsTable.table.schema.create
    )
    Await.result(dbConfig.db.run(setupAction), 5.seconds)
  }

  override def beforeEach(): Unit = {
    Await.result(dbConfig.db.run(ShipmentsTable.table.delete), 5.seconds)
  }

  "SlickShipmentReadRepository" should {

    "findByTrackingNumber" should {
      "return Some(Shipment) when tracking number exists" in {
        val shipment = createTestShipment(tracking = "FIND-ME")

        Await.result(writeRepo.create(shipment), 5.seconds)
        val result = Await.result(readRepo.findByTrackingNumber("FIND-ME"), 5.seconds)

        result.map(_.id) shouldBe Some(shipment.id)
      }

      "return None when tracking number does not exist" in {
        val result = Await.result(readRepo.findByTrackingNumber("GHOST-123"), 5.seconds)
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

        Await.result(writeRepo.create(inTransit), 5.seconds)
        Await.result(writeRepo.create(delivered), 5.seconds)

        val result = Await.result(readRepo.getByStatus(ShipmentStatus.InTransit), 5.seconds)

        result should have size 1
        result.head.trackingNumber shouldBe Some("T1")
      }
    }

    "getById" should {
      "return the shipment for a valid UUID" in {
        val shipment = createTestShipment()
        Await.result(writeRepo.create(shipment), 5.seconds)

        val result = Await.result(readRepo.getById(shipment.id), 5.seconds)
        result.map(_.id) shouldBe Some(shipment.id)
      }
    }

    "listAll" should {
      "return empty list when no data exists" in {
        val result = Await.result(readRepo.listAll(), 5.seconds)
        result shouldBe empty
      }
    }
  }
}