package scala.domain.repository.read

import infrastructure.persistence.tables.UsersTable
import mappers.UserMapper
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.read.UserSlickReadRepository
import repositories.write.SlickUserWriteRepository
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.domain.helpers.UserTestHelpers

class SlickUserReadRepositoryIT extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with UserTestHelpers {

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  /**
   * Configures a fake application with an in-memory H2 database.
   * MODE=PostgreSQL ensures UUIDs and other Postgres-specific types behave correctly.
   */
  override def fakeApplication() =
    GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:test_read_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> ""
      ).build()

  // --- Dependencies ---
  // Lazy vals prevent initialization until the Guice Application has fully started

  lazy val readRepo = app.injector.instanceOf[UserSlickReadRepository]
  lazy val writeRepo = app.injector.instanceOf[SlickUserWriteRepository]
  lazy val mapper = app.injector.instanceOf[UserMapper]
  lazy val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]

  import dbConfig.profile.api._

  /**
   * One-time setup: Create the database schema before running the test suite.
   */
  override def beforeAll(): Unit = {
    val setupAction = DBIO.seq(
      UsersTable.table.schema.dropIfExists,
      UsersTable.table.schema.create
    )
    Await.result(dbConfig.db.run(setupAction), 5.second)
  }

  /**
   * Isolation: Clean the users table before each test to prevent cross-test data pollution.
   */
  override def beforeEach(): Unit = {
    Await.result(dbConfig.db.run(UsersTable.table.delete), 5.second)
  }

  "SlickUserReadRepository" should {
    "find user by id" in {
      val user = createTestUser // Get a fresh user instance

      // Use the injected class instance to convert domain User to UsersRow
      val row = mapper.toRow(user)

      // Seed data directly via Slick to bypass repository side-effects
      Await.result(dbConfig.db.run(UsersTable.table += row), 5.seconds)

      //  Query the user
      val result = Await.result(readRepo.findUserbyId(userId), 5.seconds)


      // 3. Assertions
      result shouldBe defined
      result.get.id shouldBe userId
      result.get.email shouldBe username
    }
    "find user by email address" in{
      val newUser = createTestUser
      val row = mapper.toRow(newUser)

      Await.result(dbConfig.db.run(UsersTable.table += row), 5.second)

      val result = Await.result(readRepo.findUserByEmail(username), 5.second)
      result.map(_.email) shouldBe Some(newUser.email)
    }
    "find user by role" in {
      val newUser = createTestUser
      val newRow = mapper.toRow(newUser)

      Await.result(dbConfig.db.run(UsersTable.table += newRow), 5.second)

      val result = Await.result(readRepo.findUserByRole(role),5.second)
      result should not be empty

      // Verify role mapping and filtering logic
      result.map(_.role).head shouldBe newUser.role
      result.forall(_.role == newUser.role) shouldBe true
    }
    "list all users" in {
      val userA = createTestUser.copy(id = java.util.UUID.randomUUID(), name = "doodoo", email = "doodoo@test.com")
      val userB = createTestUser.copy(id = java.util.UUID.randomUUID(), name = "logistics", email = "logistics@test.com")

      val setup = DBIO.seq(
        UsersTable.table += mapper.toRow(userA),
        UsersTable.table += mapper.toRow(userB)
      )
      Await.result(dbConfig.db.run(setup), 5.seconds)

      // Test pagination and alphabetical sorting
      val result = Await.result(readRepo.listAllUsers(0, 10), 5.seconds)

      result should have size 2
      result.head.name shouldBe "doodoo"
    }
  }

}
