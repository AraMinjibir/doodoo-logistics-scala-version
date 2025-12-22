package repositories.read

import domain.models.{User, UsersRole}

import java.util.UUID
import scala.concurrent.Future

trait UserReadRepository {
def findUserbyId(id: UUID): Future[Option[User]]
def findUserByEmail(email:String):Future[Option[User]]
def findUserByRole(role:UsersRole) :Future[Seq[User]]
def listAllUsers(offset: Int, limit: Int): Future[Seq[User]]
}
