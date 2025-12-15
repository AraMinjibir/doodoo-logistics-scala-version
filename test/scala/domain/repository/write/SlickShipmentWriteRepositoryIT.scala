
import domain.models._
import infrastructure.persistence.models.ShipmentRow
import infrastructure.persistence.tables.ShipmentsTable
import mappers.ShipmentRowMapper
import org.scalatest.BeforeAndAfterAll
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
    with BeforeAndAfterAll {

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

  // Obtain Slick database via Play DI (OFFICIAL WAY)
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

  // Test data
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

  // Schema setup
  override def beforeAll(): Unit = {
    super.beforeAll()
    Await.result(db.run(ShipmentsTable.table.schema.create), 5.seconds)
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
}