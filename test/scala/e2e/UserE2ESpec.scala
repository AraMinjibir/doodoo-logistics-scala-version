package scala.e2e

import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UserE2ESpec extends PlaySpec
  with GuiceOneServerPerSuite
  with ForAllTestContainer{

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
    "Auth System E2E" should {

      "handle full authentication lifecycle" in {

        val wsClient = app.injector.instanceOf[WSClient]
        val baseUrl = s"http://localhost:$port/users"

        // SIGN UP

        val signUpPayload = Json.obj(
          "name" -> "DooDoo User",
          "email" -> "doodoo@gmail.com",
          "password" -> "strongPassword123",
          "phone" -> "07022223456",
          "role" -> "Admin"
        )

        val signUpResponse = Await.result(
          wsClient.url(s"$baseUrl/signUp")
            .addHttpHeaders("Content-Type" -> "application/json")
            .post(signUpPayload),
          10.seconds
        )

        signUpResponse.status mustBe 201

        val signUpJson: JsValue = signUpResponse.json
        val userId = (signUpJson \ "id").as[String]
        val token = (signUpJson \ "token").as[String]

        // 2 LOGIN

        val loginPayload = Json.obj(
          "email" -> "doodoo@gmail.com",
          "hashPassword" -> "strongPassword123"
        )

        val loginResponse = Await.result(
          wsClient.url(s"$baseUrl/login")
            .addHttpHeaders("Content-Type" -> "application/json")
            .post(loginPayload),
          10.seconds
        )

        loginResponse.status mustBe 200

        val loginJson = loginResponse.json
        val jwt = (loginJson \ "token").as[String]

        jwt must not be empty

        val adminPayload: JsObject = Json.obj(
          "name" -> "Admin User",
          "email" -> "admin@mail.com",
          "password" -> "password123",
          "phone" -> "07000000002",
          "role" -> "Admin"
        )

//        Create Admin
        Await.result(wsClient.url(s"$baseUrl/signUp").post(adminPayload), 5.seconds)


//        Login the Admin
        def loginAndGetUser(wsClient: WSClient, email: String, password: String, port: Int): (String, String) = {
          val response = Await.result(
            wsClient
              .url(s"http://localhost:$port/users/login")
              .post(Json.obj(
                "email" -> email,
                "hashPassword" -> password
              )),
            5.seconds
          )

          println("LOGIN RESPONSE: " + response.body)

          response.status mustBe OK

          val token = (response.json \ "token").as[String]
          val id = (response.json \ "id").as[String]

          (token, id)
        }



        val (adminToken, adminId) =
          loginAndGetUser(wsClient, "admin@mail.com", "password123", port)

        //  GET USER (Protected)
        val getUserResponse = Await.result(
          wsClient.url(s"$baseUrl/id/$userId")
            .addHttpHeaders("Authorization" -> s"Bearer $adminToken")
            .get(),
          10.seconds
        )


        println("user by id" + getUserResponse.status)
        println("user by id" + getUserResponse.body)
        getUserResponse.status mustBe 200

        // 4️ UPDATE USER

        val updatePayload = Json.obj(
          "name" -> "Updated User",
          "email" -> "doodoo@gmail.com",
          "password" -> "strongPassword123",
          "phone" -> "07000000000",
          "role" -> "Admin"
        )

        val updateResponse = Await.result(
          wsClient.url(s"$baseUrl/update/$userId")
            .addHttpHeaders("Authorization" -> s"Bearer $adminToken")
            .put(updatePayload),
          10.seconds
        )

        updateResponse.status mustBe 200

        // 5️ DELETE USER

        val deleteResponse = Await.result(
          wsClient.url(s"$baseUrl/$userId")
            .addHttpHeaders("Authorization" -> s"Bearer $adminToken")
            .delete(),
          10.seconds
        )

        deleteResponse.status mustBe 204
      }
    }


}
