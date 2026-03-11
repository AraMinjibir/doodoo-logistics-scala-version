package repositories

import domain.models.{User, UserRole, UserStatus}

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

trait UserRepository {

  def createUser(user: User): Future[Try[Int]]
  def findUserById(id: UUID): Future[Option[User]]

  def findUserByEmail(email: String): Future[Option[User]]
  def findUserByRole(role: UserRole): Future[Seq[User]]

  def findUserByStatus(status: UserStatus): Future[Seq[User]]
  def listAllUsers: Future[Seq[User]]

  def updateUser(user: User): Future[Try[Int]]
  def deleteUser(id: UUID): Future[Try[Int]]


}
