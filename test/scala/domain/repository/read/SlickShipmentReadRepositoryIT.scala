package scala.domain.repository.read

import domain.models.{Address, Dimensions, PackageDetails, Recipient, Shipment, ShipmentStatus}
import infrastructure.persistence.models.ShipmentRow
import mappers.ShipmentRowMapper
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import repositories.read.SlickShipmentReadRepository
import repositories.write.SlickShipmentWriteRepository
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.time.Instant
import java.util.UUID
import scala.collection.immutable.Seq
import scala.concurrent.{Await, ExecutionContext}
import scala.math.BigDecimal

class SlickShipmentReadRepositoryIT extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach{

  implicit val ec: ExecutionContext = ExecutionContext.global
  val now = Instant.now()

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
        history = Json.toJson(domain.history).toString()
      )
      }
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
  val readRepo = new SlickShipmentReadRepository(
    dbConfig.profile,
    dbConfig.db,
    mapper
  )

//  Test fixture
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

//  Shipments for List all test
  val shipmentA = validShipment.copy(
    id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
    trackingNumber = Some("LIST-A-101"),
    senderName = "List All Sender A",
    status = ShipmentStatus.Created
  )
  val shipmentB = validShipment.copy(
    id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
    trackingNumber = Some("LIST-B-202"),
    senderName = "List All Sender B",
    status = ShipmentStatus.InTransit
  )

  // Shipment 1: The target status (InTransit)
  val shipmentInTransitA = validShipment.copy(
    id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
    trackingNumber = Some("TRACK-IT-A"),
    status = ShipmentStatus.InTransit,
    senderName = "Target A"
  )

  // Shipment 2: A different status (Delivered) - Should be excluded
  val shipmentDelivered = validShipment.copy(
    id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    trackingNumber = Some("TRACK-DLV-B"),
    status = ShipmentStatus.Delivered,
    senderName = "Excluded B"
  )

  // Shipment 3: Another matching status (InTransit) - Should be included
  val shipmentInTransitB = validShipment.copy(
    id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
    trackingNumber = Some("TRACK-IT-C"),
    status = ShipmentStatus.InTransit,
    senderName = "Target C"
  )

  val writeRepo = new SlickShipmentWriteRepository(
    dbConfig.profile,
    dbConfig.db,
    mapper
  )

  // Lifecycle hooks implementation
  import infrastructure.persistence.tables.ShipmentsTable

  override def beforeAll(): Unit = {
    super.beforeAll()

    // Drop the table first, then create it.
    val setupAction = DBIO.seq(
      ShipmentsTable.table.schema.dropIfExists,
      ShipmentsTable.table.schema.create
    )

    Await.result(db.run(setupAction), 5.seconds)
  }

  override def beforeEach(): Unit = {
    // Clear the data before each test for isolation
    Await.result(db.run(ShipmentsTable.table.delete), 5.seconds)
  }

  "SlickShipmentReadRepository.findByTrackingNumber" should {

    "return Some(Shipment) when a record with the tracking number exists" in {
      // 1. Arrange: Inserting the known shipment data
      // Using the write repository to set up the state
      Await.result(writeRepo.create(validShipment), 5.seconds)

      // The tracking number to query
      val targetTrackingNumber = validShipment.trackingNumber.get

      // 2. Act: Calling the read repository method
      val result = Await.result(readRepo.findByTrackingNumber(targetTrackingNumber), 5.seconds)

      // 3. Assert: Verifying the result is present and matches the original data
      result shouldBe defined
      val foundShipment = result.get

      // Verifying key fields to ensure correct mapping
      foundShipment.id shouldBe validShipment.id
      foundShipment.trackingNumber shouldBe validShipment.trackingNumber
      foundShipment.senderName shouldBe validShipment.senderName
      foundShipment.status shouldBe validShipment.status
    }

    "return None when no record with the tracking number exists" in {
      // 1. Arrange: The table is empty due to beforeEach()

      // The tracking number that does not exist
      val nonExistentTrackingNumber = "NON-EXISTENT-456"

      // 2. Act: Call the read repository method
      val result = Await.result(readRepo.findByTrackingNumber(nonExistentTrackingNumber), 5.seconds)

      // 3. Assert: Verifying the result is None
      result shouldBe None
    }

    "SlickShipmentReadRepository.findByStatus" should {

      "return only shipments matching the specified status and exclude others" in {
        // 1. Arrange: Insert all test shipments
        Await.result(writeRepo.create(shipmentInTransitA), 5.seconds)
        Await.result(writeRepo.create(shipmentDelivered), 5.seconds)
        Await.result(writeRepo.create(shipmentInTransitB), 5.seconds)

        // Sanity check: Ensure three rows are in the DB
        val totalRows = Await.result(db.run(ShipmentsTable.table.result), 5.seconds)
        totalRows should have size 3

        // 2. Act: Query for the target status
        val targetStatus = ShipmentStatus.InTransit
        val result = Await.result(readRepo.getByStatus(targetStatus), 5.seconds)

        // 3. Assert: Verify the results

        // A. Size check: Only the two InTransit shipments should be returned
        result should have size 2

        // B. Content check: Verify that the returned list contains the IDs of the two InTransit shipments
        val foundIds = result.map(_.id).toSet

        foundIds should contain(shipmentInTransitA.id)
        foundIds should contain(shipmentInTransitB.id)

        // C. Exclusion check: Verify the Delivered shipment is NOT included
        foundIds should not contain(shipmentDelivered.id)

        // D. Property check: Verify all returned shipments actually have the correct status
        result.forall(_.status == targetStatus) shouldBe true
      }

      "return an empty list when no shipments match the status" in {
        // 1. Arrange: Insert one shipment with a non-matching status
        // Table is cleared by beforeEach, then we insert a Delivered shipment
        Await.result(writeRepo.create(shipmentDelivered), 5.seconds)

        // 2. Act: Query for a status that does not exist in the DB (e.g., 'Cancelled')
        val targetStatus = ShipmentStatus.Cancelled
        val result = Await.result(readRepo.getByStatus(targetStatus), 5.seconds)

        // 3. Assert: The result should be an empty sequence
        result shouldBe empty
      }
    }

    "SlickShipmentReadRepository.findById" should {

      "return Some(Shipment) when a record with the ID exists" in {
        // 1. Arrange: Insert the known shipment data
        // Table is clean from beforeEach, so this is the only row
        Await.result(writeRepo.create(validShipment), 5.seconds)

        // The ID to query
        val targetId = validShipment.id

        // 2. Act: Call the read repository method
        val result = Await.result(readRepo.getById(targetId), 5.seconds)

        // 3. Assert: Verify the result is present and matches the original data
        result shouldBe defined
        val foundShipment = result.get

        // Verify key properties
        foundShipment.id shouldBe validShipment.id
        foundShipment.senderName shouldBe validShipment.senderName
        foundShipment.status shouldBe validShipment.status

      }

      "return None when no record with the ID exists" in {
        // 1. Arrange: The table is empty due to beforeEach()

        // The ID that does not exist
        val nonExistentId = UUID.fromString("99999999-9999-9999-9999-999999999999")

        // 2. Act: Call the read repository method
        val result = Await.result(readRepo.getById(nonExistentId), 5.seconds)

        // 3. Assert: Verify the result is None
        result shouldBe None
      }
    }

    "SlickShipmentReadRepository.listAll" should {

      "return a list of all inserted shipments" in {
        // 1. Arrange: Insert the two distinct shipments
        Await.result(writeRepo.create(shipmentA), 5.seconds)
        Await.result(writeRepo.create(shipmentB), 5.seconds)

        // Sanity check: Total rows in DB should be 2
        val totalRows = Await.result(db.run(ShipmentsTable.table.result), 5.seconds)
        totalRows should have size 2

        // 2. Act: Call the read repository method
        val result = Await.result(readRepo.listAll(), 5.seconds)

        // 3. Assert: Verify the results

        // A. Size check: The list should contain exactly two elements
        result should have size 2

        // B. Content check: Verify that the returned list contains the IDs of both inserted shipments
        val foundIds = result.map(_.id).toSet

        foundIds should contain(shipmentA.id)
        foundIds should contain(shipmentB.id)

        // C. Integrity check: Verify one key property of one shipment for successful mapping
        result.find(_.id == shipmentA.id).map(_.senderName) shouldBe Some(shipmentA.senderName)
      }

      "return an empty list when the database is empty" in {
        // 1. Arrange: The table is already empty due to beforeEach()

        // 2. Act: Call the read repository method
        val result = Await.result(readRepo.listAll(), 5.seconds)

        // 3. Assert: The result should be an empty sequence
        result shouldBe empty
      }
    }
  }



}
