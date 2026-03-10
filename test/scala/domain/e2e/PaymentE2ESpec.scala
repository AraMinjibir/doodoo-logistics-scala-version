package scala.domain.e2e

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import domain.gateways.{MockPaymentGateway, PaymentGateway}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.http.Status.{CREATED, NO_CONTENT, OK}

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.domain.helpers.{PaymentTestHelper, ShipmentTestHelpers}

class PaymentE2ESpec
  extends PlaySpec
    with GuiceOneServerPerSuite
    with ForAllTestContainer
    with ShipmentTestHelpers
    with PaymentTestHelper {

  override val container: PostgreSQLContainer = PostgreSQLContainer("postgres:16-alpine")

  implicit lazy val ec = app.actorSystem.dispatcher

  override def fakeApplication() = {
    new GuiceApplicationBuilder()
      .overrides(
        bind[PaymentGateway].to[MockPaymentGateway],

      )
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.PostgresProfile$",
        "slick.dbs.default.db.driver" -> "org.postgresql.Driver",
        "slick.dbs.default.db.url" -> container.jdbcUrl,
        "slick.dbs.default.db.user" -> container.username,
        "slick.dbs.default.db.password" -> container.password,
        "play.evolutions.db.default.enabled" -> "true",
        "play.evolutions.db.default.autoApply" -> "true",
        "paystack.secret" -> "test-secret",
        "paystack.webhookSecret" -> "test-secret",

        "play.filters.csrf.checkHttpHeader" -> false,
        "play.filters.csrf.cookie.name" -> ""
      )
      .build()
  }

  "Payment System E2E" should {

    "cover the full payment lifecycle" in {

      val wsClient = app.injector.instanceOf[WSClient]
      val baseUrl = s"http://localhost:$port/payments"

      // 0. Create a shipment first
      val shipmentResponse = Await.result(
        wsClient.url(s"http://localhost:$port/shipments")
          .post(validCreatePayload),
        5.seconds
      )
      shipmentResponse.status mustBe CREATED
      val shipmentId = (shipmentResponse.json \ "id").as[String]

      val customerId = UUID.randomUUID()

      val createPayload = Json.obj(
        "customerId" -> customerId.toString,
        "shipmentId" -> shipmentId,
        "amount" -> 5000,
        "paymentMethod" -> "Card"
      )

      // 1. CREATE PAYMENT
      val createResponse = Await.result(
        wsClient.url(s"$baseUrl/makePayment")
          .addQueryStringParameters("callbackUrl" -> "http://localhost/test-callback")
          .post(createPayload),
        5.seconds
      )



      createResponse.status mustBe CREATED
      val reference = (createResponse.json \ "referenceNumber").as[String]

      // 2. GET PAYMENT BY REF
      val getResponse = Await.result(
        wsClient.url(s"$baseUrl/$reference").get(),
        5.seconds
      )
      getResponse.status mustBe OK
      (getResponse.json \ "status").as[String] mustBe "Pending"

      // 3. WEBHOOK UPDATE (Mock will succeed)
      val webhookPayload = Json.obj(
        "event" -> "charge.success",
        "data" -> Json.obj("reference" -> reference)
      )
      val webhookResponse = Await.result(
        wsClient.url(s"http://localhost:$port/webhooks/payments")
          .post(webhookPayload),
        5.seconds
      )
      webhookResponse.status mustBe OK

      // 4. VERIFY STATUS UPDATED
      val updatedPayment = Await.result(
        wsClient.url(s"$baseUrl/$reference").get(),
        5.seconds
      )
      (updatedPayment.json \ "status").as[String] mustBe "Successful"

      // 5. GET BY STATUS
      val statusResponse = Await.result(
        wsClient.url(s"$baseUrl/status/Successful").get(),
        5.seconds
      )


      statusResponse.status mustBe OK

      // 6. GET REVENUE
      val today = java.time.LocalDate.now.toString
      val revenueResponse = Await.result(
        wsClient.url(s"$baseUrl/revenue/daily")
          .addQueryStringParameters("date" -> today)
          .get(),
        5.seconds
      )


      revenueResponse.status mustBe OK

      // 7. DELETE PAYMENT
      val deleteResponse = Await.result(
        wsClient.url(s"$baseUrl/$reference").delete(),
        5.seconds
      )

      deleteResponse.status mustBe NO_CONTENT
    }
  }
}