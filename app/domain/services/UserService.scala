package domain.services

import domain.errors.DomainError
import domain.models.{User, UserRole, UserStatus}

import java.util.UUID
import scala.concurrent.Future

trait UserService {
  def registerUser(user:User): Future[Either[DomainError, User]]
  def login(email: String, password: String): Future[Either[DomainError, String]]

  def findUserById(id:UUID): Future[Option[User]]
  def findUserByRole(role:UserRole): Future[Seq[User]]
  def findUserByStatus(status:UserStatus): Future[Seq[User]]
  def findUserByUsername(username:String): Future[Option[User]]
  def listAllUsers: Future[Seq[User]]

  def updateUserDetails(userId:UUID, user: User): Future[Either[DomainError,User]]
  def updateUserStatus(userId: UUID,status: UserStatus): Future[Either[DomainError,User]]
  def deleteUser(id: UUID): Future[Either[DomainError,Unit]]

}
