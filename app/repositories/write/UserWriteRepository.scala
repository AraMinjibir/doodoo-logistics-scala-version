package repositories.write

import domain.models.User

import java.util.UUID
import scala.concurrent.Future

trait UserWriteRepository {
  def createUser(user: User): Future[User]
  def updateUser(user: User): Future[Int]
  def deleteUser(id: UUID): Future[Int]
}
