package repositories

import domain.models.{User, UsersRole}

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

trait UserRepository {
  def createUser(user: User): Future[Try[User]]
  def updateUser(user: User): Future[Try[Int]]
  def deleteUser(id: UUID): Future[Try[Int]]
  def findUserbyId(id: UUID): Future[Option[User]]
  def findUserByEmail(email:String):Future[Option[User]]
  def findUserByRole(role:UsersRole) :Future[Seq[User]]
  def listAllUsers(offset: Int, limit: Int): Future[Seq[User]]
}
