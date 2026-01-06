package domain.services.impl

import com.google.inject.{Inject, Singleton}
import domain.models.errors.DomainError
import domain.models.{User, UserUpdateData, UsersRole}
import domain.services.UserService
import domain.validation.UserValidation
import org.mindrot.jbcrypt.BCrypt
import domain.models.errors.DomainError._
import repositories.UserRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class UserServiceImpl @Inject()(
                                 repo:UserRepository,
                                 validator:UserValidation)(implicit ec: ExecutionContext) extends UserService{

  override def createUser(user: User): Future[Either[DomainError, User]] = {
    // 1. Structural Validation (Uses DomainError from validator)
    validator.validateUserCreation(user) match {
      case Left(error) => Future.successful(Left(error))
      case Right(_) =>
        // 2. Business Logic: Check uniqueness
        repo.findUserByEmail(user.email).flatMap {
          case Some(_) => Future.successful(Left(EmailAlreadyTaken))
          case None =>
            Future {
              val hashedPassword = BCrypt.hashpw(user.hashPassword, BCrypt.gensalt())
              User(
                id = UUID.randomUUID(),
                name = user.name,
                email = user.email,
                role = user.role,
                hashPassword = hashedPassword,
                phoneNumber = user.phoneNumber
              )
            }.flatMap { newUser =>
              repo.createUser(newUser).map[Either[DomainError, User]]{
                case Success(createdUser) => Right(createdUser)
                case Failure(ex) => Left(DomainError.DatabaseError(ex.getMessage))
              }
            }
        }
    }
  }

  override def getUserById(userId: UUID): Future[Option[User]] = repo.findUserbyId(userId)

  override def getUserByEmail(email: String): Future[Option[User]] = repo.findUserByEmail(email)

  override def getUserByRole(userRole: UsersRole): Future[Seq[User]] = repo.findUserByRole(userRole)

  override def getAllUsers(offset: Int, limit: Int): Future[Seq[User]] = repo.listAllUsers(offset, limit)

  override def updateUser(
                           userId: UUID,
                           user:UserUpdateData
                         ): Future[Either[DomainError, User]] = {

    repo.findUserbyId(userId).flatMap {
      case None =>
        Future.successful(Left(UserNotFound))

      case Some(existingUser) =>
        val updatedUser = existingUser.copy(
          name = user.name,
          email = user.email,
          phoneNumber = user.phoneNumber,
          role = user.role
        )

        validator.validateUserUpdate(updatedUser) match {
          case Left(error) =>
            Future.successful(Left(error))

          case Right(_) =>
            val emailCheck =
              if (updatedUser.email != existingUser.email)
                repo.findUserByEmail(updatedUser.email)
              else
                Future.successful(None)

            emailCheck.flatMap {
              case Some(_) =>
                Future.successful(Left(EmailAlreadyTaken))

              case None =>
                repo.updateUser(updatedUser).map {
                  case Success(1) => Right(updatedUser)
                  case Success(0) => Left(DatabaseError(UserNotFound.message))
                  case Failure(ex) => Left(DatabaseError(ex.getMessage))
                }
            }
        }
    }.recover {
      case e =>
        Left(DatabaseError(e.getMessage))
    }
  }

  override def deleteUser(userId: UUID): Future[Either[DomainError, User]] = {
    repo.findUserbyId(userId).flatMap {
      case None => Future.successful(Left(UserNotFound))
      case Some(user) =>
        repo.deleteUser(userId).map {
          case Success(0) =>
            Left(DomainError.GenericError("Delete failed: user not found"))

          case Success(_) =>
            Right(user)

          case Failure(ex) =>
            Left(DomainError.DatabaseError(ex.getMessage))
        }

    }
  }
}
