package domain.services

import api.dto.{UsersCreationDto, UsersUpdateDto}
import domain.models.errors.DomainError
import domain.models.{User, UsersRole}

import java.util.UUID
import scala.concurrent.Future

trait UserService {

  def createUser(dto:UsersCreationDto): Future[Either[DomainError, User]]
  def getUserById(userId:UUID): Future[Option[User]]
  def getUserByRole(userRole:UsersRole):Future[Seq[User]]
  def getUserByEmail(email:String): Future[Option[User]]
  def getAllUsers(offset:Int, limit: Int): Future[Seq[User]]

  def updateUser(userId:UUID, updateData: UsersUpdateDto): Future[Either[DomainError, User]]
  def deleteUser(userId:UUID):Future[Either[DomainError, User]]

}
