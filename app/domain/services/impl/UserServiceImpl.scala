package domain.services.impl

import api.dto.{UsersCreationDto, UsersUpdateDto}
import com.google.inject.{Inject, Singleton}
import domain.models.errors.DomainError
import domain.models.{User, UsersRole}
import domain.services.UserService
import domain.validation.UserValidation
import org.mindrot.jbcrypt.BCrypt
import repositories.read.UserReadRepository
import repositories.write.UserWriteRepository
import domain.models.errors.DomainError._
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserServiceImpl @Inject()(
                               writeRepo:UserWriteRepository,
                               readRepo:UserReadRepository,
                               validator:UserValidation)(implicit ec: ExecutionContext) extends UserService{

  override def createUser(dto: UsersCreationDto): Future[Either[DomainError, User]] = {
    // 1. Structural Validation (Uses DomainError from validator)
    validator.validateUserCreation(dto) match {
      case Left(error) => Future.successful(Left(error))
      case Right(_) =>
        // 2. Business Logic: Check uniqueness
        readRepo.findUserByEmail(dto.email).flatMap {
          case Some(_) => Future.successful(Left(EmailAlreadyTaken))
          case None =>
            Future {
              val hashedPassword = BCrypt.hashpw(dto.hashPassword, BCrypt.gensalt())
              User(
                id = UUID.randomUUID(),
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

  override def updateUser(userId: UUID, dto: UsersUpdateDto): Future[Either[DomainError, User]] = {
    readRepo.findUserbyId(userId).flatMap {
      case None =>
        Future.successful(Left(UserNotFound): Either[DomainError, User])

      case Some(existingUser) =>
        validator.validateUserUpdate(dto) match {
          case Left(error) =>
            Future.successful(Left(error): Either[DomainError, User])

          case Right(_) =>
            val emailChanged = dto.email != existingUser.email
            val emailCheck = if (emailChanged) readRepo.findUserByEmail(dto.email) else Future.successful(None)

            emailCheck.flatMap {
              case Some(_) =>
                Future.successful(Left(EmailAlreadyTaken): Either[DomainError, User])
              case None =>
                val updatedUser = existingUser.copy(
                  name = dto.name,
                  email = dto.email,
                  phoneNumber = dto.phoneNumber,
                  role = dto.role
                )
                writeRepo.updateUser(updatedUser).map(_ => Right(updatedUser): Either[DomainError, User])
            }
        }
    }.recover {
      // Explicitly cast the recover result as well
      case e: Exception => Left(DatabaseError(e.getMessage)): Either[DomainError, User]
    }
  }

  override def deleteUser(userId: UUID): Future[Either[DomainError, User]] = {
    readRepo.findUserbyId(userId).flatMap {
      case None => Future.successful(Left(UserNotFound))
      case Some(user) =>
        writeRepo.deleteUser(userId).map {
          case n if n > 0 => Right(user)
          case _          => Left(GenericError("Delete failed: User may have already been removed"))
        }
    }.recover {
      case e: Exception => Left(DatabaseError(e.getMessage))
    }
  }
}
