package domain.services.impl

import com.google.inject.{Inject, Singleton}
import controllers.helpers.ResultMapper
import domain.errors.{DomainError, InvalidCredentials, UpdateUserError, UserAlreadyExists, UserDeletionError, UserNotFound, UserNotFoundWithId, UserStatusIsNotActive, UserStatusUpdateError, ValidationError}
import domain.models.{User, UserAccountUpdated, UserCreated, UserRole, UserStatus}
import domain.services.{EventBus, JwtService, UserService}
import repositories.UserRepository

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class UserServiceImpl @Inject()(
                            userRepository: UserRepository,
                            jwt:JwtService,
                            eventBus: EventBus
                            )(implicit ex: ExecutionContext) extends UserService with ResultMapper {

 override def registerUser(user: User): Future[Either[DomainError, User]] = {
   val hashedPassword = User.hashPasswordValue(user.hashPassword)
    userRepository.findUserByEmail(user.email).flatMap {

      case Some(_) =>
        Future.successful(Left(UserAlreadyExists(user.email)))

      case None =>
        User.createUser(
          name = user.name,
          email = user.email,
          password = hashedPassword,
          phone = user.phone,
          role = user.role
        ) match {

          case Left(validationErrors) =>
            Future.successful(Left(ValidationError(validationErrors.mkString(" , "))))

          case Right(newUser) =>
            userRepository.createUser(newUser).map {
              case Success(_) =>
                eventBus.publish(
                  UserCreated(
                    username = newUser.email,
                    role = newUser.role,
                    status = newUser.status
                  )
                )
                Right(newUser)
              case Failure(ex) => Left(mapInsertException(ex))
            }
        }
    }
  }
 override def login(email: String, password: String): Future[Either[DomainError, String]] = {

    userRepository.findUserByEmail(email).map {

      case None =>
        Left(UserNotFound(email))

      case Some(user) =>
        if (user.status != UserStatus.Active) {
          Left(UserStatusIsNotActive(user.status))
        } else if (!User.checkPassword(password, user.hashPassword)) {
          Left(InvalidCredentials())
        } else {
          val token = jwt.generateToken(user)
          Right(token)
        }
    }
  }

  override def findUserById(id:UUID): Future[Option[User]] = userRepository.findUserById(id)
  override def findUserByUsername(username:String): Future[Option[User]] = userRepository.findUserByEmail(username)
  override def findUserByRole(role:UserRole): Future[Seq[User]] = userRepository.findUserByRole(role)
  override def findUserByStatus(status:UserStatus): Future[Seq[User]] = userRepository.findUserByStatus(status)
  override def listAllUsers: Future[Seq[User]] = userRepository.listAllUsers

  override def updateUserDetails(userId:UUID, user: User): Future[Either[DomainError,User]] = {
    userRepository.findUserById(userId).flatMap{
      case None =>
        Future.successful(Left(UserNotFoundWithId(userId)))

      case Some(existingUser) =>
        val updatedUser = existingUser.copy(
          name = user.name,
          email = user.email,
          hashPassword = user.hashPassword,
          phone = user.phone,
          role = user.role,
          status = user.status,
          createdAt = existingUser.createdAt,
          updatedAt = Some(Instant.now())
        )
        userRepository.updateUser(updatedUser).map{
          case Success(_) => Right(updatedUser)
          case Failure(ex) => Left(UpdateUserError(ex.getMessage))
        }
    }
  }
  override def updateUserStatus(userId: UUID,status: UserStatus): Future[Either[DomainError,User]] = {
    userRepository.findUserById(userId).flatMap{
      case None =>
        Future.successful(Left(UserNotFoundWithId(userId)))

      case Some(user) =>
        val updatedStatus = user.copy(
          status = status,
          updatedAt = Some(Instant.now())
        )
        userRepository.updateUser(updatedStatus).map{
          case Success(_) =>
            eventBus.publish(
              UserAccountUpdated(
                userId = updatedStatus.id,
                email = updatedStatus.email,
                status = updatedStatus.status
              )
            )
            Right(updatedStatus)
          case Failure(ex) => Left(UserStatusUpdateError(ex.getMessage))
        }
    }
  }
 override def deleteUser(userId: UUID): Future[Either[DomainError,Unit]] = {
   userRepository.deleteUser(userId).map {
     case Success(1) => Right(())
     case Success(0) => Left(UserNotFoundWithId(userId))
     case Failure(ex) => Left(UserDeletionError(ex.getMessage))
   }
  }


}
