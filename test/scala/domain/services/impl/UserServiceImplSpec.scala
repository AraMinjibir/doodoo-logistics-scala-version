package scala.domain.services.impl

import domain.errors.{InvalidCredentials, UserNotFound, UserNotFoundWithId, UserStatusIsNotActive}
import domain.models.{User, UserStatus}
import domain.models.UserRole.Admin
import domain.services.JwtService
import domain.services.impl.UserServiceImpl
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import repositories.UserRepository

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
class UserServiceImplSpec extends AnyWordSpec
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach{

  val mockRepo: UserRepository = mock[UserRepository]
  val jwt: JwtService = mock[JwtService]
  val service = new UserServiceImpl(mockRepo, jwt)
  val hashedPassword: String = User.hashPasswordValue("doodoooaauiiq1234")

  val user: User = User.createUser(
    name = "DooDoo User",
    email = "DooDooUser@gmail.com",
    password = hashedPassword,
    phone = "07022223456",
    role = Admin
  ).fold(
    errors => throw new RuntimeException(errors.mkString(",")),
    identity
  )

  override def beforeEach(): Unit = {
    reset(mockRepo)
  }

  "UserServiceImpl" should {
    "Successfully create a new user" in{
      when(mockRepo.findUserByEmail(any()))
        .thenReturn(Future.successful(None))

      when(mockRepo.createUser(any()))
        .thenReturn(Future.successful(Success(1)))

      val result = Await.result(service.registerUser(user), 5.second)
      result.isRight shouldBe true
      result.toOption.get.email shouldBe user.email

      verify(mockRepo).createUser(any())
    }
    "Successfully login the user" should {
      "return UserNotFound when email does not exist" in {

        when(mockRepo.findUserByEmail("test@email.com"))
          .thenReturn(Future.successful(None))

        val result = Await.result(service.login("test@email.com", "password"), 5.seconds)

        result shouldBe Left(UserNotFound("test@email.com"))
      }
      "return UserStatusIsNotActive when user is inactive" in {

        val inactiveUser = user.copy(status = UserStatus.Suspended)

        when(mockRepo.findUserByEmail(inactiveUser.email))
          .thenReturn(Future.successful(Some(inactiveUser)))

        val result = Await.result(service.login(inactiveUser.email, "password"), 5.seconds)

        result shouldBe Left(UserStatusIsNotActive(inactiveUser.status))
      }
      "return InvalidCredentials when password is incorrect" in {

        when(mockRepo.findUserByEmail(user.email))
          .thenReturn(Future.successful(Some(user)))

        val result = Await.result(service.login(user.email, "wrongPassword"), 5.seconds)

        result shouldBe Left(InvalidCredentials())
      }
      "return token when login is successful" in {

        val token = "mock-jwt-token"

        when(mockRepo.findUserByEmail(user.email))
          .thenReturn(Future.successful(Some(user)))

        when(jwt.generateToken(user))
          .thenReturn(token)

        val result = Await.result(service.login(user.email, "doodoooaauiiq1234"), 5.seconds)

        result shouldBe Right(token)

        verify(jwt).generateToken(user)
      }
    }
    "findUserById should return user when found" in {

      val userId = UUID.randomUUID()

      when(mockRepo.findUserById(userId))
        .thenReturn(Future.successful(Some(user)))

      val result = Await.result(service.findUserById(userId), 5.seconds)

      result shouldBe Some(user)

      verify(mockRepo).findUserById(userId)
    }
    "findUserByUsername should return user when found" in {

      when(mockRepo.findUserByEmail(user.email))
        .thenReturn(Future.successful(Some(user)))

      val result = Await.result(service.findUserByUsername(user.email), 5.seconds)

      result shouldBe Some(user)

      verify(mockRepo).findUserByEmail(user.email)
    }
    "findUserByRole should return users with given role" in {

      val users = Seq(user)

      when(mockRepo.findUserByRole(user.role))
        .thenReturn(Future.successful(users))

      val result = Await.result(service.findUserByRole(user.role), 5.seconds)

      result shouldBe users

      verify(mockRepo).findUserByRole(user.role)
    }
    "findUserByStatus should return users with given status" in {

      val users = Seq(user)

      when(mockRepo.findUserByStatus(user.status))
        .thenReturn(Future.successful(users))

      val result = Await.result(service.findUserByStatus(user.status), 5.seconds)

      result shouldBe users

      verify(mockRepo).findUserByStatus(user.status)
    }
    "listAllUsers should return all users" in {

      val users = Seq(user)

      when(mockRepo.listAllUsers)
        .thenReturn(Future.successful(users))

      val result = Await.result(service.listAllUsers, 5.seconds)

      result shouldBe users

      verify(mockRepo).listAllUsers
    }
    "update user details should return" should{
      "UserNotFoundWithId when user does not exist" in {
        val userId = UUID.randomUUID()

        when(mockRepo.findUserById(userId))
          .thenReturn(Future.successful(None))

        val result = Await.result(service.updateUserDetails(userId, user), 5.seconds)

        result shouldBe Left(UserNotFoundWithId(userId))

        verify(mockRepo).findUserById(userId)      }
      "updateUserDetails should update user successfully" in {

        val userId = user.id

        when(mockRepo.findUserById(userId))
          .thenReturn(Future.successful(Some(user)))

        when(mockRepo.updateUser(any()))
          .thenReturn(Future.successful(Success(1)))

        val result = Await.result(service.updateUserDetails(userId, user), 5.seconds)

        result.isRight shouldBe true

        verify(mockRepo).updateUser(any())
      }
      "updateUserDetails should return UpdateUserError when repository fails" in {

        val userId = user.id

        when(mockRepo.findUserById(userId))
          .thenReturn(Future.successful(Some(user)))

        when(mockRepo.updateUser(any()))
          .thenReturn(Future.successful(Failure(new RuntimeException("DB error"))))

        val result = Await.result(service.updateUserDetails(userId, user), 5.seconds)

        result.isLeft shouldBe true
      }
    }
    "update user status should return" should{
      "return UserNotFoundWithId when user does not exist" in {

        val userId = UUID.randomUUID()

        when(mockRepo.findUserById(userId))
          .thenReturn(Future.successful(None))

        val result = Await.result(service.updateUserStatus(userId, UserStatus.Active), 5.seconds)

        result shouldBe Left(UserNotFoundWithId(userId))
      }
      "update user status successfully" in {

        val userId = user.id

        when(mockRepo.findUserById(userId))
          .thenReturn(Future.successful(Some(user)))

        when(mockRepo.updateUser(any()))
          .thenReturn(Future.successful(Success(1)))

        val result = Await.result(service.updateUserStatus(userId, UserStatus.Suspended), 5.seconds)

        result.isRight shouldBe true

        verify(mockRepo).updateUser(any())
      }
      "return UserStatusUpdateError when repository fails" in {

        val userId = user.id

        when(mockRepo.findUserById(userId))
          .thenReturn(Future.successful(Some(user)))

        when(mockRepo.updateUser(any()))
          .thenReturn(Future.successful(Failure(new RuntimeException("DB error"))))

        val result = Await.result(service.updateUserStatus(userId, UserStatus.Suspended), 5.seconds)

        result.isLeft shouldBe true
      }

    }
    "deleteUser" should {
      "return delete user successfully" in {

      val userId = UUID.randomUUID()

      when(mockRepo.deleteUser(userId))
        .thenReturn(Future.successful(Success(1)))

      val result = Await.result(service.deleteUser(userId), 5.seconds)

      result shouldBe Right(())

      verify(mockRepo).deleteUser(userId)
    }
      "return UserNotFoundWithId when no rows affected" in {

      val userId = UUID.randomUUID()

      when(mockRepo.deleteUser(userId))
        .thenReturn(Future.successful(Success(0)))

      val result = Await.result(service.deleteUser(userId), 5.seconds)

      result shouldBe Left(UserNotFoundWithId(userId))
    }
    }

  }

}
