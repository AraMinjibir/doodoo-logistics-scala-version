package scala.domain.api

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, CREATED, NOT_FOUND, OK}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.libs.json._
import play.api.inject.guice.GuiceApplicationBuilder

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.domain.helpers.ShipmentTestHelpers

class ShipmentApiSpec extends PlaySpec
  with GuiceOneAppPerSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with ShipmentTestHelpers {

  override def beforeEach(): Unit = {
    // Get the database instance from the injector
    val dbConfig = app.injector.instanceOf[play.api.db.slick.DatabaseConfigProvider].get[slick.jdbc.JdbcProfile]
    import dbConfig.profile.api._

    // Manually truncate the table before every test
    val deleteAction = infrastructure.persistence.tables.ShipmentsTable.table.delete
    Await.result(dbConfig.db.run(deleteAction), 5.seconds)
  }
  override def beforeAll(): Unit = {
    val dbConfig = app.injector.instanceOf[play.api.db.slick.DatabaseConfigProvider].get[slick.jdbc.JdbcProfile]
    import dbConfig.profile.api._
    // Create tables
    Await.result(dbConfig.db.run(infrastructure.persistence.tables.ShipmentsTable.table.schema.create), 5.seconds)
  }

  override def fakeApplication() =
    GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile"     -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver"   -> "org.h2.Driver",
        "slick.dbs.default.db.url"      -> "jdbc:h2:mem:test_api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user"     -> "sa",
        "slick.dbs.default.db.password" -> ""
      ).build()

  "Shipment API" should {

    "create a new shipment via POST" in {
      val request = FakeRequest(POST, "/shipments").withJsonBody(validCreatePayload)
      val result = route(app, request).get

      status(result) mustBe CREATED
      (contentAsJson(result) \ "senderName").as[String] mustBe "Ara Minjibir"
    }

    "update status via PATCH using PathBindable" in {
      val payload = Json.obj("status" -> "InTransit", "location" -> "Kano Warehouse")
      val request = FakeRequest(PATCH, "/shipments/tracking/NON-EXISTENT-999/status").withJsonBody(payload)

      status(route(app, request).get) mustBe NOT_FOUND
    }

    "return 404 for a non-existent shipment" in {
      val result = route(app, FakeRequest(GET, s"/shipments/${UUID.randomUUID()}")).get
      status(result) mustBe NOT_FOUND
    }

    "return 200 and a list of shipments" in {
      seedShipment(app)

      val result = route(app, FakeRequest(GET, "/shipments")).get
      status(result) mustBe OK
      (contentAsJson(result) \ "data").as[JsArray].value must not be empty
    }

    "successfully bind the URL string to a ShipmentStatus object" in {
      seedShipment(app) // Default status is 'Created'

      val result = route(app, FakeRequest(GET, "/shipments/status/Created")).get
      status(result) mustBe OK
    }

    "return 400 Bad Request if the status string is invalid" in {
      val result = route(app, FakeRequest(GET, "/shipments/status/InvalidStatus")).get
      status(result) mustBe BAD_REQUEST
    }
  }
}