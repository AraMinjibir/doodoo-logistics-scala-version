package scala.domain.e2e

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
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

  override def fakeApplication(): Application = {
    // These values are generated dynamically by Docker
    new GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.PostgresProfile$",
        "slick.dbs.default.db.driver" -> "org.postgresql.Driver",
        "slick.dbs.default.db.url" -> container.jdbcUrl,
        "slick.dbs.default.db.user" -> container.username,
        "slick.dbs.default.db.password" -> container.password,
        "play.evolutions.db.default.enabled" -> "true",
        "play.evolutions.db.default.autoApply" -> "true",

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
      val usersUrl = s"http://localhost:$port/users/signUp"

      Await.result(wsClient.url(usersUrl).post(senderPayload), 5.seconds)
      Await.result(wsClient.url(usersUrl).post(adminPayload), 5.seconds)
      Await.result(wsClient.url(usersUrl).post(providerPayload), 5.seconds)
      Await.result(wsClient.url(usersUrl).post(recipientPayload), 5.seconds)
      Await.result(wsClient.url(usersUrl).post(agentPayload), 5.seconds)

      val (senderToken, senderId) =
        loginAndGetUser(wsClient, "sender@mail.com", "password123", port)

      val (adminToken, adminId) =
        loginAndGetUser(wsClient, "admin@mail.com", "password123", port)

      val (providerToken, providerId) =
        loginAndGetUser(wsClient, "provider@mail.com", "password123", port)

      val (recipientToken, recipientId) =
        loginAndGetUser(wsClient, "recipient@mail.com", "password123", port)


      //Create Shipment (Sender Only)
      val createResponse = Await.result(
        wsClient
          .url(baseUrl)
          .addHttpHeaders("Authorization" -> s"Bearer $senderToken")
          .post(validCreatePayload),
        10.seconds
      )
      createResponse.status mustBe CREATED

      //      Extract Tracking Number + ShipmentId

      val trackingNumber =
        (createResponse.json \ "trackingNumber").as[String]

      val shipmentId =
        (createResponse.json \ "id").as[String]

      //      Assign Service Provider (Admin)

      val assignResponse = Await.result(
        wsClient
          .url(s"$baseUrl/$shipmentId/assign/$providerId")
          .addHttpHeaders("Authorization" -> s"Bearer $adminToken")
          .patch(Json.obj()),
        5.seconds
      )

      assignResponse.status mustBe OK


      //      Update Shipment Status (ServiceProvider)
      val AssignedResponse = Await.result(
        wsClient
          .url(s"$baseUrl/tracking/$trackingNumber/status")
          .addHttpHeaders("Authorization" -> s"Bearer $providerToken")
          .patch(Json.obj("status" -> "InTransit")),
        5.seconds
      )

      AssignedResponse.status mustBe OK

      Await.result(
        wsClient
          .url(s"$baseUrl/tracking/$trackingNumber/status")
          .addHttpHeaders("Authorization" -> s"Bearer $providerToken")
          .patch(Json.obj("status" -> "OutForDelivery")),
        5.seconds
      ).status mustBe OK

      Await.result(
        wsClient
          .url(s"$baseUrl/tracking/$trackingNumber/status")
          .addHttpHeaders("Authorization" -> s"Bearer $providerToken")
          .patch(Json.obj("status" -> "Delivered")),
        5.seconds
      ).status mustBe OK

      //      Upload Proof (Recipient)
      val proofResponse = Await.result(
        wsClient
          .url(s"$baseUrl/tracking/$trackingNumber/proof-of-delivery")
          .addHttpHeaders("Authorization" -> s"Bearer $recipientToken")
          .post(proofPayload),
        5.seconds
      )

      proofResponse.status mustBe OK
      (proofResponse.json \ "status").as[String] mustBe "Delivered"

      //      Track Shipment (Sender or Recipient)
      val getResponse = Await.result(
        wsClient
          .url(s"$baseUrl/tracking/$trackingNumber")
          .addHttpHeaders("Authorization" -> s"Bearer $senderToken")
          .get(),
        5.seconds
      )

      getResponse.status mustBe OK


    }
  }
}