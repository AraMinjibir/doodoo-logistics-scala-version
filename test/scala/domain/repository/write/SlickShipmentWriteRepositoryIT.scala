
import domain.models._
import infrastructure.persistence.models.ShipmentRow
import infrastructure.persistence.tables.ShipmentsTable
import mappers.ShipmentRowMapper
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.write.SlickShipmentWriteRepository
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import java.time.Instant
import java.util.UUID

class SlickShipmentWriteRepositoryIT
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // Play test application with test database config
  override def fakeApplication() =
    GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> "",
        "slick.dbs.default.db.connectionPool" -> "HikariCP"
      )
      .build()

  // Obtain Slick database via Play DI
  lazy val dbConfig: DatabaseConfig[JdbcProfile] =
    app.injector
      .instanceOf[DatabaseConfigProvider]
      .get[JdbcProfile]

  import dbConfig.profile.api._
  lazy val db = dbConfig.db

  // Mapper (simple inline implementation for test)
  val mapper: ShipmentRowMapper = new ShipmentRowMapper {
    override def toRow(domain: Shipment): ShipmentRow =
      ShipmentRow(
        id = domain.id,
        trackingNumber = domain.trackingNumber,
        senderName = domain.senderName,
        recipientName = domain.recipient.name,
        recipientAddress = domain.recipient.address,
        recipientContact = domain.recipient.contact,
        weight = domain.packageDetails.weight,
        length = domain.packageDetails.dimensions.length,
        width = domain.packageDetails.dimensions.width,
        height = domain.packageDetails.dimensions.height,
        contents = domain.packageDetails.contents,
        status = domain.status,
        estimatedDeliveryDate = domain.estimatedDeliveryDate,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt,
        cost = domain.cost,
        history = domain.history.toString
      )
  }

  // Repository under test
  val repo = new SlickShipmentWriteRepository(
    dbConfig.profile,
    dbConfig.db,
    mapper
  )

  // Test fixtures

  val now = Instant.now()
  val validShipment = Shipment(
    id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
    trackingNumber = Some("GET-TRACK-123"),
    senderName = "Ara Minjibir",
    recipient = Recipient(
      name = "Test Recipient",
      address = Address(
        street = "123 Main St",
        city = "Mjb",
        state = "Kano",
        country = "Nigeria",
        postalCode = "100001"
      ),
      contact = "08012345678"
    ),
    packageDetails = PackageDetails(
      weight = 30.0,
      dimensions = Dimensions(30.0, 20.0, 10.0),
      contents = "Clothing"
    ),
    status = ShipmentStatus.Created,
    estimatedDeliveryDate = None,
    createdAt = now,
    updatedAt = now,
    cost = BigDecimal(5000),
    history = Seq.empty
  )
  val updatedShipment = validShipment.copy(
    senderName = "Ara Minjibir Updated",
    status = ShipmentStatus.OutForDelivery,
    updatedAt = Instant.now()
  )

  // Schema setup
  override def beforeAll(): Unit = {
    super.beforeAll()

    // drop action
    val dropAction = ShipmentsTable.table.schema.dropIfExists

    // create action
    val createAction = ShipmentsTable.table.schema.create

    // 1. Drop the table if it exists (safe)
    // 2. Then, create the table (safe)
    val setupAction = DBIO.seq(dropAction, createAction)

    // Execute the combined action
    Await.result(db.run(setupAction), 5.seconds)
  }

    // Clear table before *each* test case for isolation
  override def beforeEach(): Unit = {
    Await.result(db.run(ShipmentsTable.table.delete), 5.seconds)
  }

  // Tests
  "SlickShipmentWriteRepository.create" should {

    "persist a shipment row into the database" in {
      val result = Await.result(repo.create(validShipment), 5.seconds)
      result shouldBe 1

      val rows = Await.result(db.run(ShipmentsTable.table.result), 5.seconds)
      rows should have size 1
      rows.head.senderName shouldBe validShipment.senderName
    }
  }

  "SlickShipmentWriteRepository.update" should {

    "successfully update an existing shipment in the database" in {
      // 1. Arrange: Insert the initial shipment data

      val createResult = Await.result(repo.create(validShipment), 5.seconds)
      createResult shouldBe 1 // Sanity check: one row was inserted

      // 2. Act: Call the update method with the modified object
      val updateResult = Await.result(repo.update(updatedShipment), 5.seconds)
      updateResult shouldBe 1 // The update should affect exactly one row

      // 3. Assert: Read the row back from the DB and verify the changes

      // A. Define the query to fetch the row by its ID
      val query = ShipmentsTable.table.filter(_.id === validShipment.id)

      // B. Execute the query and get the result
      val rows = Await.result(db.run(query.result), 5.seconds)

      // C. Verify the row count and the actual changes
      rows should have size 1
      val persistedRow = rows.head

      // Verify the updated fields
      persistedRow.senderName shouldBe updatedShipment.senderName
      persistedRow.status shouldBe updatedShipment.status

      // Verify the ID is unchanged (best practice sanity check)
      persistedRow.id shouldBe validShipment.id

      // Verify a non-updated field remains the same
      persistedRow.trackingNumber shouldBe validShipment.trackingNumber
    }
  }

  "SlickShipmentWriteRepository.delete" should {

    "successfully delete an existing shipment from the database" in {
      // 1. Arrange: Insert the shipment data to ensure a row exists
      val createResult = Await.result(repo.create(validShipment), 5.seconds)
      createResult shouldBe 1 // Sanity check: one row was inserted

      // Quick verification: Check the row is definitely there before delete
      val initialRows = Await.result(db.run(ShipmentsTable.table.result), 5.seconds)
      initialRows should have size 1

      // 2. Act: Call the delete method using the shipment's ID
      val deleteResult = Await.result(repo.delete(validShipment.id), 5.seconds)
      deleteResult shouldBe 1 // The delete should affect exactly one row

      // 3. Assert: Read the row back from the DB and verify it is gone

      // A. Define the query to fetch the row by its ID
      val query = ShipmentsTable.table.filter(_.id === validShipment.id)

      // B. Execute the query and get the result
      val finalRows = Await.result(db.run(query.result), 5.seconds)

      // C. Verify the row count: it must be zero
      finalRows should have size 0
    }

    "return 0 when attempting to delete a non-existent shipment" in {
      // Act: Attempt to delete an ID that was never inserted
      val nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999")

      val deleteResult = Await.result(repo.delete(nonExistentId), 5.seconds)

      // Assert: No rows should be affected, so the result is 0
      deleteResult shouldBe 0

      // Further assert: Ensure the table is still empty (from beforeEach)
      val rows = Await.result(db.run(ShipmentsTable.table.result), 5.seconds)
      rows should have size 0
    }
  }
}