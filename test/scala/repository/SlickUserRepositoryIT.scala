package scala.repository

import infrastructure.persistence.tables.{UserTable, ShipmentsTable, SupportCenterTable}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import repositories.UserRepository
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}
import domain.models.{User, UserRole, UserStatus}
import domain.models.UserRole.{Admin, Recipient}
import domain.models.UserStatus.Suspended
import play.api.Application
import infrastructure.tables.{ShipmentsTable, SupportCenterTable}

class SlickUserRepositoryIT
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterAll
    with BeforeAndAfterEach{

  lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
        "slick.dbs.default.db.driver" -> "org.h2.Driver",
        "slick.dbs.default.db.url" -> "jdbc:h2:mem:test_write_repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "slick.dbs.default.db.user" -> "sa",
        "slick.dbs.default.db.password" -> ""
      ).build()

  lazy val repo: UserRepository = app.injector.instanceOf[UserRepository]

  lazy val dbConfig = app.injector.instanceOf[DatabaseConfigProvider].get[JdbcProfile]
  import dbConfig.profile.api._

 override def beforeAll(): Unit = {
  super.beforeAll()

  val reset = DBIO.seq(
    SupportCenterTable.table.schema.dropIfExists,
    ShipmentsTable.table.schema.dropIfExists,
    UserTable.table.schema.dropIfExists,

    UserTable.table.schema.create,
    ShipmentsTable.table.schema.create,
    SupportCenterTable.table.schema.create
  )

  Await.result(dbConfig.db.run(reset), 10.seconds)
}

 override def beforeEach(): Unit = {
  super.beforeEach()

  val reset = DBIO.seq(
    SupportCenterTable.table.schema.dropIfExists,
    ShipmentsTable.table.schema.dropIfExists,
    UserTable.table.schema.dropIfExists,

    UserTable.table.schema.create,
    ShipmentsTable.table.schema.create,
    SupportCenterTable.table.schema.create
  )

  Await.result(dbConfig.db.run(reset), 10.seconds)
}

  val newUser: User = User.createUser(
    name = "DooDoo User",
    email = "DooDooUser@gmail.com",
    password = "doodoooaauiiq1234",
    phone = "07022223456",
    role = Admin
  ).fold(
    errors => throw new RuntimeException(errors.mkString(",")),
    identity
  )
  val newUser2: User = User.createUser(
    name = "DooDoo User2",
    email = "DooDooUser2@gmail.com",
    password = "doodoouser2112eiiq1234",
    phone = "07022223456",
    role = Recipient
  ).fold(
    errors => throw new RuntimeException(errors.mkString(",")),
    identity
  )

  "SlickUserRepository" should {
    "Persist a new user row into the database" in {

      val result =  Await.result(repo.createUser(newUser), 5.second)
      result.isSuccess shouldBe true

      val persisted = Await.result(dbConfig.db.run(UserTable.table.result), 5.second)

      persisted should have size 1
      persisted.head.status shouldBe newUser.status
      persisted.head.id shouldBe newUser.id
      persisted.head.role shouldBe newUser.role
    }
    "Successfully update user status to Suspended" in{
      Await.result(repo.createUser(newUser), 5.second)

      val newStatus = newUser.copy(
        id = newUser.id,
        name = newUser.name,
        email = newUser.email,
        hashPassword = newUser.hashPassword,
        phone = newUser.phone,
        role = newUser.role,
        status = Suspended,
        createdAt = newUser.createdAt,
        updatedAt = newUser.updatedAt
      )

      val result = Await.result(repo.updateUser(newStatus), 5.second)
      result.isSuccess shouldBe true

      val updatedRow = Await.result(dbConfig.db.run(UserTable.table.filter(_.id === newUser.id).result.head), 5.second)

      updatedRow.status shouldBe UserStatus.Suspended
    }
    "Return a user with a given valid id" in {
      Await.result(repo.createUser(newUser), 5.second)

      val result = Await.result(repo.findUserById(newUser.id),5.second)

      result.map(_.id) shouldBe Some(newUser.id)
    }
    "Return None when user id does not exist" in {
      val randomId = java.util.UUID.randomUUID()

      val result = Await.result(repo.findUserById(randomId), 5.seconds)

      result shouldBe None
    }
    "Retrieve a user with a given Role" in{
      Await.result(repo.createUser(newUser), 5.second)

      val result = Await.result(repo.findUserByRole(newUser.role), 5.second)
      result.head.role shouldBe UserRole.Admin
    }
    "Return a user with a given email " in{
      Await.result(repo.createUser(newUser), 5.second)

      val result = Await.result(repo.findUserByEmail(newUser.email), 5.second)
      result.head.email shouldBe newUser.email

    }
    "Retrieve users by status" in {
      Await.result(repo.createUser(newUser), 5.seconds)

      val result = Await.result(repo.findUserByStatus(newUser.status), 5.seconds)

      result should not be empty
    }
    "Return all users inserted" in{
      val user1 = newUser
      val user2 = newUser2

      Await.result(repo.createUser(user1), 5.second)
      Await.result(repo.createUser(user2), 5.second)

      val result = Await.result(repo.listAllUsers, 5.second)

      result should have size 2
    }
    "Return empty when no user found" in {
      val result =  Await.result(repo.listAllUsers, 5.second)

      result shouldBe Seq.empty
    }
  }

}
