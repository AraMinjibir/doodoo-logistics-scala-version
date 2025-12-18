package repositories.write

import domain.models._
import infrastructure.persistence.tables.ShipmentsTable
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import slick.jdbc.JdbcProfile
import scala.domain.helpers.ShipmentTestHelpers

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import java.util.UUID

class SlickShipmentWriteRepositoryIT
  extends AnyWordSpec
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
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:test_write_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> ""
      ).build()

  lazy val repo     = app.injector.instanceOf[SlickShipmentWriteRepository]
  lazy val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]
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

  "SlickShipmentWriteRepository" should {

    "create" should {
      "persist a shipment row into the database" in {
        val shipment = createTestShipment() // One-liner data creation

        val result = Await.result(repo.create(shipment), 5.seconds)
        result shouldBe 1

        val rows = Await.result(dbConfig.db.run(ShipmentsTable.table.result), 5.seconds)
        rows should have size 1
        rows.head.senderName shouldBe shipment.senderName
      }
    }

    "update" should {
      "successfully update an existing shipment" in {
        // Arrange
        val shipment = createTestShipment()
        Await.result(repo.create(shipment), 5.seconds)

        // Act - Modify only what you need for the test
        val updated = shipment.copy(senderName = "Updated Name", status = ShipmentStatus.InTransit)
        val result = Await.result(repo.update(updated), 5.seconds)

        // Assert
        result shouldBe 1
        val persisted = Await.result(dbConfig.db.run(ShipmentsTable.table.filter(_.id === shipment.id).result.head), 5.seconds)
        persisted.senderName shouldBe "Updated Name"
        persisted.status shouldBe ShipmentStatus.InTransit
      }
    }

    "delete" should {
      "successfully remove a shipment" in {
        // Arrange
        val shipment = createTestShipment()
        Await.result(repo.create(shipment), 5.seconds)

        // Act
        val result = Await.result(repo.delete(shipment.id), 5.seconds)

        // Assert
        result shouldBe 1
        val rows = Await.result(dbConfig.db.run(ShipmentsTable.table.result), 5.seconds)
        rows shouldBe empty
      }

      "return 0 when deleting a non-existent shipment" in {
        val result = Await.result(repo.delete(UUID.randomUUID()), 5.seconds)
        result shouldBe 0
      }
    }
  }
}