package domain.services.impl

import api.dto.{UsersCreationDto, UsersUpdateDto}
import com.google.inject.{Inject, Singleton}
import domain.models.{User, UsersRole}
import domain.services.UserService
import domain.validation.UserValidation
import org.mindrot.jbcrypt.BCrypt
import repositories.read.UserReadRepository
import repositories.write.UserWriteRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserServiceImpl @Inject()(
                               writeRepo:UserWriteRepository,
                               readRepo:UserReadRepository,
                               validator:UserValidation)(implicit ec: ExecutionContext) extends UserService{

  override def createUser(dto: UsersCreationDto): Future[Either[String, User]] = {
    // 1. Structural Validation
    validator.validateUserCreation(dto) match {
      case Left(error) => Future.successful(Left(error))
      case Right(_) =>
        // 2. Business Logic: Check uniqueness
        readRepo.findUserByEmail(dto.email).flatMap {
          case Some(_) => Future.successful(Left("Email address already registered"))
          case None =>
            // Offload CPU-heavy hashing to a Future
            Future {
              val hashedPassword = BCrypt.hashpw(dto.hashPassword, BCrypt.gensalt())

              User(
                id = java.util.UUID.randomUUID(),
                name = dto.name,
                email = dto.email,
                role = dto.role,
                hashPassword = hashedPassword,
                phoneNumber = dto.phoneNumber
              )
            }.flatMap { newUser =>
              writeRepo.createUser(newUser).map(Right(_))
            }
    }
  }
}

  override def getUserById(userId: UUID): Future[Option[User]] = readRepo.findUserbyId(userId)

  override def getUserByEmail(email: String): Future[Option[User]] = readRepo.findUserByEmail(email)

  override def getUserByRole(userRole: UsersRole): Future[Seq[User]] = readRepo.findUserByRole(userRole)

  override def getAllUsers(offset: Int, limit: Int): Future[Seq[User]] = readRepo.listAllUsers(offset, limit)

  override def updateUser(userId: UUID, dto: UsersUpdateDto): Future[Either[String, User]] = {
    readRepo.findUserbyId(userId).flatMap{
      case None => Future.successful(Left(s"User with id: $userId is not found"))
      case Some(existingUser) =>

        // 1. Validation
        validator.validateUserUpdate(dto) match {
          case Left(error) => Future.successful(Left(error))
          case Right(_) =>

            // 2. Email Uniqueness
            val emailChanged = dto.email != existingUser.email
            val emailCheck = if (emailChanged) readRepo.findUserByEmail(dto.email) else Future.successful(None)

            emailCheck.flatMap {
              case Some(_) => Future.successful(Left("Email already taken by another user"))
              case None =>
                // 3. Map to Domain: Keep the existingUser
                val updatedUser = existingUser.copy(
                  name = dto.name,
                  email = dto.email,
                  phoneNumber = dto.phoneNumber,
                  role = dto.role
                )

                writeRepo.updateUser(updatedUser).map(_ => Right(updatedUser))
            }
        }
    }.recover {
      case e: Exception => Left(s"Database error: ${e.getMessage}")
    }
    }

  override def deleteUser(userId: UUID): Future[Either[String, User]] = {
    // 1. First, find the user so we have the data to return
    readRepo.findUserbyId(userId).flatMap {
      case None =>
        // User doesn't exist, return early
        Future.successful(Left(s"User with ID $userId not found"))

      case Some(user) =>
        // 2. User exists, proceed to delete
        writeRepo.deleteUser(userId).map {
          case n if n > 0 =>
            // Successfully deleted at least 1 row
            Right(user)

          case _ =>
            // This handles the rare edge case where the user was deleted
            // by another process between our read and write
            Left("Delete failed: User may have already been removed")
        }
    }
  }
}
