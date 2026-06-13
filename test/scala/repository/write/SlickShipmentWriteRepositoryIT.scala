package scala.repository.write

import domain.models.ShipmentStatus.Assigned
import domain.models._
import infrastructure.persistence.tables.{ShipmentsTable, UserTable}
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.{SlickShipmentRepository, SlickUserRepository}
import slick.jdbc.JdbcProfile

import java.time.Instant
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import java.util.UUID
import scala.helpers.ShipmentTestHelpers
import scala.util.Success

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

  lazy val repo: SlickShipmentRepository = app.injector.instanceOf[SlickShipmentRepository]
  lazy val userRepo: SlickUserRepository = app.injector.instanceOf[SlickUserRepository]

  lazy val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]
  import dbConfig.profile.api._

  override def beforeAll(): Unit = {
    val setupAction = DBIO.seq(
      ShipmentsTable.table.schema.dropIfExists,
      UserTable.table.schema.dropIfExists,


      UserTable.table.schema.create,
      ShipmentsTable.table.schema.create
    )
    Await.result(dbConfig.db.run(setupAction), 5.seconds)
  }

  override def beforeEach(): Unit = {
    Await.result(dbConfig.db.run( DBIO.seq(
      ShipmentsTable.table.delete,
      UserTable.table.delete)
    ), 5.seconds)
  }

  "SlickShipmentWriteRepository" should {

    "create" should {
      "persist a shipment row into the database" in {
        val shipment = createTestShipment()

        val result = Await.result(repo.create(shipment), 5.seconds)
        result shouldBe Success(1)

        val rows = Await.result(dbConfig.db.run(ShipmentsTable.table.result), 5.seconds)
        rows should have size 1
        rows.head.senderName shouldBe shipment.senderName
      }
    }

    "update" should {
      "successfully update an existing shipment" in {
        // Arrange
        val newUser = serviceProvider
        Await.result(userRepo.createUser(newUser), 5.second)

        val shipment = createTestShipment()
        Await.result(repo.create(shipment), 5.seconds)

        // Act - Modify only what you need for the test
        val updated = shipment.copy(senderName = "Updated Name", status = ShipmentStatus.InTransit)
        val result = Await.result(repo.update(updated), 5.seconds)

        // Assert
        result shouldBe Success(1)
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
        result shouldBe Success(1)
        val rows = Await.result(dbConfig.db.run(ShipmentsTable.table.result), 5.seconds)
        rows shouldBe empty
      }

      "return 0 when deleting a non-existent shipment" in {
        val result = Await.result(repo.delete(UUID.randomUUID()), 5.seconds)
        result shouldBe Success (0)
      }
    }

    "upload" should{
      "successfully upload a proof of delivery column in the shipment row" in {
        val proof = uploadProofOfDelivery
        val shipment = createTestShipment()

        Await.result(repo.create(shipment), 5.second)
        val result = Await.result(repo.uploadProofOfDelivery(shipmentId,proof), 5.second)
        val saved = result.value

        saved.id shouldBe shipment.id
        saved.trackingNumber shouldBe shipment.trackingNumber
        saved.status shouldBe shipment.status
        saved.proofOfDelivery shouldBe List(proof)
        saved.recipient shouldBe shipment.recipient
        saved.packageDetails shouldBe shipment.packageDetails
        saved.senderName shouldBe shipment.senderName

        val rows = Await.result(dbConfig.db.run(ShipmentsTable.table.result), 5.second)
        rows.head.proofOfDelivery shouldBe List(proof)
      }
    }

    "assignServiceProvider" should {

      "assign a service provider successfully" in {
        // Arrange
        val shipment = createTestShipment()
        Await.result(repo.create(shipment), 5.seconds)

        val newNewServiceProvider = serviceProvider
        Await.result(userRepo.createUser(newNewServiceProvider), 5.seconds)

        // Act
        val result = Await.result(repo.assignServiceProvider(shipment.id, serviceProvider.id), 5.seconds)

        // Assert
        result shouldBe a[Success[_]]

        val persisted = Await.result(
          dbConfig.db.run(ShipmentsTable.table.filter(_.id === shipment.id).result.head),
          5.seconds
        )

        persisted.serviceProviderId shouldBe Some(serviceProvider.id)
        persisted.status shouldBe ShipmentStatus.Assigned
      }

      "fail if the user is not a service provider" in {
        // Arrange
        val shipment = createTestShipment()
        Await.result(repo.create(shipment), 5.seconds)

        val customer = User(
          id = providerId,
          name = "Customer Agent",
          email = "agent@test.com",
          hashPassword = "password123",
          phone = "123456789",
          role = UserRole.CustomerSupportAgent,
          status = UserStatus.Active,
          createdAt = Instant.now(),
          updatedAt = None
        )
        Await.result(userRepo.createUser(customer), 5.seconds)

        // Act
        val result = Await.result(repo.assignServiceProvider(shipment.id, customer.id), 5.seconds)

        // Assert
        result.isFailure shouldBe true
        result.failed.get.getMessage should include("User is not a service provider")
      }

      "fail if the shipment does not exist" in {
        // Arrange
        val newServiceProvider = serviceProvider
        Await.result(userRepo.createUser(newServiceProvider), 5.seconds)

        val nonExistentShipmentId = UUID.randomUUID()

        // Act
        val result = Await.result(repo.assignServiceProvider(nonExistentShipmentId, newServiceProvider.id), 5.seconds)

        // Assert
        result.isFailure shouldBe true
        result.failed.get.getMessage should include("No shipment")
      }

    }
    "findByServiceProvider" should {

      "return shipments assigned to the given service provider" in {
        val providerId = UUID.randomUUID()



        val newServiceProvider = serviceProvider.copy(id = providerId)
        Await.result(userRepo.createUser(newServiceProvider), 5.seconds)

        // Assign the shipment to this provider
        val shipment = createTestShipment(
          tracking = "SP-1",
          status = ShipmentStatus.Assigned,
          serviceProviderId = Some(providerId)
        )


        Await.result(repo.create(shipment), 5.seconds)


        val result = Await.result(repo.findByServiceProvider(providerId), 5.seconds)

        result should not be empty
        result.head.status shouldBe ShipmentStatus.Assigned
        result.flatMap(_.trackingNumber) should contain ("SP-1")
      }

      "return empty list if no shipments assigned to provider" in {
        val providerId = UUID.randomUUID()
        val shipment = createTestShipment()
        Await.result(repo.create(shipment), 5.seconds)

        val result = Await.result(repo.findByServiceProvider(providerId), 5.seconds)
        result shouldBe empty
      }
    }
  }


}