package scala.e2e

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.{CREATED, OK}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.helpers.{ShipmentTestHelpers, SupportCenterTestHelper}

class SupportCenterE2ESpec
  extends PlaySpec
  with GuiceOneServerPerSuite
  with ForAllTestContainer
  with SupportCenterTestHelper
  with ShipmentTestHelpers
  {

    // Container
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

    "Complaint System E2E" should {

      "cover the full complaint lifecycle including comments" in {

        val wsClient = app.injector.instanceOf[WSClient]
        val baseUrl = s"http://localhost:$port/complaints"
        val usersUrl = s"http://localhost:$port/users/signUp"

        Await.result(wsClient.url(usersUrl).post(senderPayload), 5.seconds)
        Await.result(wsClient.url(usersUrl).post(recipientPayload), 5.seconds)
        Await.result(wsClient.url(usersUrl).post(agentPayload), 5.seconds)


        val (senderToken, senderId) =
          loginAndGetUser(wsClient, "sender@mail.com", "password123", port)

        val (agentToken, agentId) =
          loginAndGetUser(wsClient,"supportagent@mail.com", "password123", port)

        val createUserResponse = Await.result(
          wsClient.url(s"http://localhost:$port/users/signUp")
            .addHttpHeaders("Authorization" -> s"Bearer $senderToken")
            .post(validUserPayload),
          10.seconds
        )

        createUserResponse.status mustBe CREATED

        val shipmentResponse = Await.result(
          wsClient.url(s"http://localhost:$port/shipments")
            .addHttpHeaders("Authorization" -> s"Bearer $senderToken")
            .post(validCreatePayload),
          10.seconds
        )

        shipmentResponse.status mustBe CREATED

        val shipmentId =
          (shipmentResponse.json \ "id").as[String]

      val  validComplaintPayload:JsValue = Json.obj(
          "userId" -> senderId,
          "shipmentId" -> shipmentId,
          "subject" ->  "Complaint",
          "description" -> "Package damaged",
        )
        // 1. CREATE COMPLAINT


        val createResponse = Await.result(
          wsClient.url(baseUrl)
            .addHttpHeaders("Authorization" -> s"Bearer $senderToken")
            .post(validComplaintPayload),
          10.seconds
        )

        createResponse.status mustBe CREATED

        val complaintId =
          (createResponse.json \ "id").as[String]


        // 2. GET BY ID


        val getResponse = Await.result(
          wsClient.url(s"$baseUrl/$complaintId")
            .addHttpHeaders("Authorization" -> s"Bearer $senderToken")
            .get(),
          10.seconds
        )

        getResponse.status mustBe OK
        (getResponse.json \ "status").as[String] mustBe "Open"


        // 3. GET ALL


        val getAllResponse = Await.result(
          wsClient.url(baseUrl)
            .addHttpHeaders("Authorization" -> s"Bearer $agentToken")
            .get(),
          10.seconds
        )

        getAllResponse.status mustBe OK
        getAllResponse.json.as[Seq[JsValue]].nonEmpty mustBe true


        // 4. GET BY STATUS


        val byStatusResponse = Await.result(
          wsClient.url(s"$baseUrl/status/Open")
            .addHttpHeaders("Authorization" -> s"Bearer $agentToken")
            .get(),
          5.seconds

        )

        byStatusResponse.status mustBe OK
        byStatusResponse.json.as[Seq[JsValue]].size must be >= 1


        // 5. MARK AS IN PROGRESS


        val inProgressResponse = Await.result(
          wsClient
            .url(s"$baseUrl/$complaintId/in-progress")
            .addHttpHeaders("Authorization" -> s"Bearer $agentToken")
            .patch(Json.obj()),
          5.seconds
        )

        inProgressResponse.status mustBe OK
        (inProgressResponse.json \ "status").as[String] mustBe "InProgress"


        // 6. ADD COMMENT

        val complaintUUID = UUID.fromString(complaintId)

        val commentPayload = Json.obj(
          "complaintId" -> complaintUUID.toString,
          "authorId" -> UUID.randomUUID().toString,
          "message" -> "We are investigating this issue."
        )


        val commentResponse = Await.result(
          wsClient
            .url(s"$baseUrl/$complaintUUID/comments")
            .addHttpHeaders("Authorization" -> s"Bearer $agentToken")
            .post(commentPayload),
          10.seconds
        )

        commentResponse.status mustBe OK
        (commentResponse.json \ "message").as[String] mustBe "We are investigating this issue."


        // 7. MARK AS RESOLVED


        val resolveResponse = Await.result(
          wsClient
            .url(s"$baseUrl/$complaintId/resolve/$agentId")
            .addHttpHeaders("Authorization" -> s"Bearer $agentToken")
            .patch(Json.obj()),
          10.seconds
        )

        resolveResponse.status mustBe OK
        (resolveResponse.json \ "status").as[String] mustBe "Resolved"


        // 8. VERIFY FINAL STATE


        val finalGet = Await.result(
          wsClient.url(s"$baseUrl/$complaintId")
            .addHttpHeaders("Authorization" -> s"Bearer $agentToken")
            .get(),
          10.seconds
        )

        (finalGet.json \ "status").as[String] mustBe "Resolved"
      }
    }
}
