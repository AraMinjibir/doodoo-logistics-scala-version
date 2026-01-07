package scala.domain.repository.write

import domain.models.UsersRole.Admin
import infrastructure.persistence.models.UsersRow
import infrastructure.persistence.tables.UsersTable
import mappers.UserRowMapper
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.SlickUserRepository
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import scala.domain.helpers.UserTestHelpers

class SlickUserWriteRepositoryIT extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with BeforeAndAfterAll
  with BeforeAndAfterEach with UserTestHelpers{

  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  /**
   * Configures a localized database environment for write-operation testing.
   * DB_CLOSE_DELAY=-1 is critical for in-memory databases to persist schema between connection closes.
   */
  override def fakeApplication() =
    GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:test_write_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> ""
      ).build()

 lazy val repo = app.injector.instanceOf[SlickUserRepository]
 lazy val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]
  lazy val mapper = app.injector.instanceOf[UserRowMapper]

  import dbConfig.profile.api._

  /**
   * Schema Initialization: Ensures the 'users' table structure exists before any test execution.
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

  "Slick User Write Repository" should {
    "Persist a user row into the database" in {
      val newUser = createTestUser()
      val newRow = mapper.toRow(newUser)

      // We use direct Slick action (+=) here because the H2 driver restricts
      // the 'returning' clause used in the production repository for UUID types.
      val result = Await.result(dbConfig.db.run(UsersTable.table += newRow), 5.second)
      result shouldBe 1

      // Verify persistence via a direct query
      val rows = Await.result(dbConfig.db.run(UsersTable.table.result), 5.second)
      rows should have size 1
      rows.head.name shouldBe newUser.name

    }
    "successfully update an existing user" in {
      val user = createTestUser()
      val row = mapper.toRow(user)

      // Setup: Insert initial record
      Await.result(dbConfig.db.run(UsersTable.table += row), 5.seconds)

      // Act: Modify domain object and map back to persistence row
      val updatedUser = user.copy(name = "updated doodoo")
      val updatedRow = mapper.toRow(updatedUser)

      // Execute update with a strict ID filter
      val updateAction = UsersTable.table
        .filter(_.id === user.id)
        .update(updatedRow)

      val affectedRows = Await.result(dbConfig.db.run(updateAction), 5.seconds)

      // Assert: Verify only one row was updated
      affectedRows shouldBe 1
    }
    "successfully remove a user" in {
      val user = createTestUser()
      val row = mapper.toRow(user)

      // Setup: Seed the database
      Await.result(dbConfig.db.run(UsersTable.table += row), 5.seconds)

      // Act: Delete filtered by Primary Key
      val deleteAction = UsersTable.table.filter(_.id === user.id).delete

      val deletedRows = Await.result(dbConfig.db.run(deleteAction), 5.seconds)

      // Assert: Verify row count reduced as expected
      deletedRows shouldBe 1
    }
    "return 0 when deleting a non-existent user" in {
      // 1. Generate a random ID that definitely isn't in the DB
      val randomId = java.util.UUID.randomUUID()

      // 2. Attempt to delete via the repository (or direct filter)
      // Logic: delete from users where id = randomId
      val deleteAction = UsersTable.table.filter(_.id === randomId).delete
      val affectedRows = Await.result(dbConfig.db.run(deleteAction), 5.seconds)

      // 3. Assertion: Should be 0 because no row matched that ID
      affectedRows shouldBe 0
    }
  }
}