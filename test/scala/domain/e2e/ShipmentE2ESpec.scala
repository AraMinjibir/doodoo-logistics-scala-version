package scala.domain.e2e

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.domain.helpers.ShipmentTestHelpers

class ShipmentE2ESpec
  extends PlaySpec
    with GuiceOneServerPerSuite
    with ForAllTestContainer
    with ShipmentTestHelpers {

  // Container (Postgres 16 on Alpine is fast and light)
  override val container: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine")

  override def fakeApplication() = {
    // These values are generated dynamically by Docker
    new GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile"            -> "slick.jdbc.PostgresProfile$",
        "slick.dbs.default.db.driver"           -> "org.postgresql.Driver",
        "slick.dbs.default.db.url"              -> container.jdbcUrl,
        "slick.dbs.default.db.user"             -> container.username,
        "slick.dbs.default.db.password"         -> container.password,
        "play.evolutions.db.default.enabled"    -> "true",
        "play.evolutions.db.default.autoApply"  -> "true",

//        connection timeout for failing
        "slick.dbs.default.db.numThreads" -> "5",
        "slick.dbs.default.db.maxConnections" -> "5",
        "slick.dbs.default.db.connectionTimeout" -> "5000", // 5 seconds
        "slick.dbs.default.db.registerMbeans" -> "true",

        "play.http.errorHandler" -> "play.api.http.DefaultHttpErrorHandler",

//        Debugger
    "logger.slick.jdbc.JdbcBackend.statement" -> "DEBUG",
    "logger.com.zaxxer.hikari" -> "DEBUG"
      )
      .build()
  }

  "Shipment System E2E" should {
    "handle a full shipment lifecycle using Testcontainers" in {
      val wsClient = app.injector.instanceOf[WSClient]
      val baseUrl = s"http://localhost:$port/shipments"

      // 1. CREATE
      val createResponse = Await.result(
        wsClient.url(baseUrl).post(validCreatePayload),
        10.seconds
      )

      if (createResponse.status != 201) {
        println(s"DEBUG - Container URL: ${container.jdbcUrl}")
        println(s"E2E FAILURE BODY: ${createResponse.body}")
      }

      createResponse.status mustBe CREATED
      val trackingNumber = (createResponse.json \ "trackingNumber").as[String]

      // 2. GET BY TRACKING
      val getResponse = Await.result(
        wsClient.url(s"$baseUrl/tracking/$trackingNumber").get(),
        5.seconds
      )

      getResponse.status mustBe OK
      (getResponse.json \ "status").as[String] mustBe "Created"

      //    list shipments with pagination

      val response = Await.result(
        wsClient.url(s"$baseUrl?page=1&pageSize=5").get(),
        5.seconds
      )

      response.status mustBe OK
      val json = response.json
      (json \ "metadata" \ "count").as[Int] must be >= 1


//      Transition to deliver

// Created → InTransit
    val inTransitResponse = Await.result(
      wsClient
        .url(s"$baseUrl/tracking/$trackingNumber/status")
        .patch(Json.obj("status" -> "InTransit")),
      5.seconds
    )

      inTransitResponse.status mustBe OK

      // InTransit → OutForDelivery
      val outForDeliveryResponse = Await.result(
        wsClient
          .url(s"$baseUrl/tracking/$trackingNumber/status")
          .patch(Json.obj("status" -> "OutForDelivery")),
        5.seconds
      )

      outForDeliveryResponse.status mustBe OK
      // InTransit → OutForDelivery
      val deliverResponse = Await.result(
        wsClient
          .url(s"$baseUrl/tracking/$trackingNumber/status")
          .patch(Json.obj("status" -> "Delivered")),
        5.seconds
      )

      println("DELIVER RESPONSE BODY:")
      println(deliverResponse.body)

      deliverResponse.status mustBe OK

      val proofResponse = Await.result(
        wsClient
          .url(s"$baseUrl/tracking/$trackingNumber/proof-of-delivery")
          .post(proofPayload),
        5.seconds
      )

      proofResponse.status mustBe OK
      (proofResponse.json \ "status").as[String] mustBe "Delivered"
    }

  }
}