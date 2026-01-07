package scala.domain.services.impl

import domain.models.UsersRole.ServiceProvider
import domain.services.impl.UserServiceImpl
import domain.validation.UserValidation
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import domain.models.errors.DomainError._
import repositories.UserRepository

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.domain.helpers.UserTestHelpers
import scala.util.Success

class UserServiceImplSpec extends AnyWordSpec
  with Matchers
  with MockitoSugar
  with BeforeAndAfterEach with UserTestHelpers{

//  Mocks

  val mockRepo = mock[UserRepository]
  val mockValidation = mock[UserValidation]

  val userService = new UserServiceImpl(
    mockRepo,mockValidation
  )

  override def beforeEach(): Unit = {
    reset( mockRepo,mockRepo,mockValidation)
  }

  "UserServiceImplSpec" should{

    "Create a new user successfully" in {
      val dto = validUserCreation()

      // 1. Mock Validation
      when(mockValidation.validateUserCreation(any())).thenReturn(Right(()))

      // 2. Mock Email Check to return None
      when(mockRepo.findUserByEmail(any())).thenReturn(Future.successful(None))

      // 3. Mock Write Repo to return a User object
      val expectedUser = createTestUser()
      when(mockRepo.createUser(any())).thenReturn(Future.successful(Success(expectedUser)))

      // Act
      val result = Await.result(userService.createUser(dto), 5.seconds)

      // Assert
      result shouldBe Right(expectedUser)
      verify(mockRepo).createUser(any())
      verify(mockRepo).findUserByEmail(dto.email)
    }
    "FAIL to create a new user when validation fails" in {
      val dto = validUserCreation()
      val validationErr = ValidationError("Name cannot be empty")
      when(mockValidation.validateUserCreation(any())).thenReturn(Left( validationErr))

      val result = Await.result(userService.createUser(dto), 5.seconds)

      // Asserting on the Left side of the Either
      result shouldBe Left( validationErr)

      // Verify that the database was never touched after validation failed
      verify(mockRepo, never()).findUserByEmail(any())
    }
    "Update user successfully" in {
      val id = userId
      val existingUser = createTestUser()
      val updateDto = validUserUpdate()

      // Create what the user SHOULD look like after update
      val expectedUpdatedUser = existingUser.copy(
        name = updateDto.name,
        email = updateDto.email,
        phoneNumber = updateDto.phoneNumber,
        role = updateDto.role
      )

      // 1. Mock finding the existing user
      when(mockRepo.findUserbyId(id))
        .thenReturn(Future.successful(Some(existingUser)))

      // 2. Mock Validation to return Right(Unit)
      when(mockValidation.validateUserUpdate(any())).thenReturn(Right(()))

      // 3. Mock the Email uniqueness check
      when(mockRepo.findUserByEmail(any())).thenReturn(Future.successful(None))

      // 4. Mock the Write Repo to return success
      when(mockRepo.updateUser(any())).thenReturn(Future.successful(Success(1)))

      // Act
      val result = Await.result(userService.updateUser(id, updateDto), 5.seconds)

      // Assert
      result shouldBe Right(expectedUpdatedUser)

      verify(mockRepo).updateUser(any())
    }
    "Fail to update user when user isn't found" in {
      val randomId = java.util.UUID.randomUUID()
      val dto = validUserUpdate()

      // Stubbing: Use any() here
      when(mockRepo.findUserbyId(any())).thenReturn(Future.successful(None))

      // Act: Use REAL values (randomId and dto) here, NOT any()
      val result = Await.result(userService.updateUser(randomId, dto), 5.second)

      // Assert: Check for the error message returned by your Impl
      result shouldBe Left(UserNotFound)
    }
    "Get user by id" in{
      val newUser = createTestUser()
      when(mockRepo.findUserbyId(userId)).thenReturn(Future.successful(Some(newUser)))

      val result = Await.result(userService.getUserById(userId), 5.second)
      result shouldBe Some(newUser)
    }
    "Return None when no user found with the given id" in{
      when(mockRepo.findUserbyId(any())).thenReturn(Future.successful(None))

      val result = Await.result(userService.getUserById(UUID.randomUUID()), 5.second)
      result shouldBe None
    }
    "Get user by email" in{
      val newUser = createTestUser()
      val userEmail = username

      when(mockRepo.findUserByEmail(userEmail)).thenReturn(Future.successful(Some(newUser)))

      val result = Await.result(userService.getUserByEmail(userEmail), 5.second)

      result shouldBe Some(newUser)

    }
    "Return none when no user found with the given email" in{
      when(mockRepo.findUserByEmail(any())).thenReturn(Future.successful(None))

      val result = Await.result(userService.getUserByEmail("@ddd"), 5.second)
      result shouldBe None
    }
    "Get user by role" in{
      val userRole = role
      val newUser = createTestUser()

      when(mockRepo.findUserByRole(userRole)).thenReturn(Future.successful(Seq(newUser)))

      val result = Await.result(userService.getUserByRole(userRole), 5.second)
      result shouldBe Seq(newUser)
    }
    "Return none when no user found with the given role" in {
      when(mockRepo.findUserByRole(any())).thenReturn(Future.successful(Seq.empty))

      val result = Await.result(userService.getUserByRole(ServiceProvider), 5.second)

      result shouldBe empty
    }
    "Get all users" in {
      val reset = 0
      val limit = 5
      val users = createTestUser()

      when(mockRepo.listAllUsers(reset, limit)).thenReturn(Future.successful(Seq(users)))

      val result = Await.result(userService.getAllUsers(reset, limit), 5.second)

      result shouldBe Seq(users)
    }
    "RETURN Empty when no users exist" in {
      when(mockRepo.listAllUsers(0,10)).thenReturn(Future.successful(Seq.empty))

      val result = Await.result(userService.getAllUsers(0,10), 5.second)
      result shouldBe empty
    }
    "Delete user" in{
      val id = userId
      val user = createTestUser()


      // 1. Stub the READ check: The service needs to find the user first
      when(mockRepo.findUserbyId(id)).thenReturn(Future.successful(Some(user)))

      // 2. Stub the WRITE operation: Simulate 1 row deleted
      when(mockRepo.deleteUser(id)).thenReturn(Future.successful(Success(1)))

      // Act
      val result = Await.result(userService.deleteUser(id), 5.seconds)

      // Assert: The service returns the User that was deleted
      result shouldBe Right(user)

      // Verify both repo methods were called
      verify(mockRepo).findUserbyId(id)
      verify(mockRepo).deleteUser(id)
    }
    "RETURN Fail when user isn't deleted" in{
      val wrongId = UUID.fromString("33333333-3333-3333-3333-333333333333")
      when(mockRepo.findUserbyId(any())).thenReturn(Future.successful(None))
      when(mockRepo.deleteUser(any())).thenReturn(Future.successful(Success(0)))

      val result = Await.result(userService.deleteUser(wrongId), 5.second)
      result.isLeft shouldBe true
    }

  }
}
