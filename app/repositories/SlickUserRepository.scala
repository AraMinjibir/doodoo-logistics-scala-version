package repositories

import com.google.inject.{Inject, Singleton}
import domain.models.{User, UserRole, UserStatus}
import infrastructure.persistence.tables.{PaymentTable, UserTable}
import mappers.UserMapper
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SlickUserRepository @Inject()(
                                     dbConfigProvider: DatabaseConfigProvider,
                                     mapper: UserMapper
                                   )(implicit ex: ExecutionContext) extends UserRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  private val db = dbConfig.db
  import dbConfig.profile.api._
  private val q = UserTable.table

  import UserTable._

 override def createUser(user: User): Future[Try[Int]] = {
   val row = mapper.fromDomain(user)
   val insert = q += row
   db.run(insert.asTry)
 }
  override def findUserById(id: UUID): Future[Option[User]] = {
    db.run(q.filter(_.id === id).result.headOption)
      .map(_.map(mapper.fromRow))
  }

  override def findUserByEmail(email: String): Future[Option[User]] =
    db.run(q.filter(_.email === email).result.headOption).map(_.map(mapper.fromRow))

  override def findUserByRole(role: UserRole): Future[Seq[User]] =
    db.run(q.filter(_.role === role).result).map(_.map(mapper.fromRow))

  override def findUserByStatus(status: UserStatus): Future[Seq[User]] =
    db.run(q.filter(_.status === status).result).map(_.map(mapper.fromRow))
  override def listAllUsers: Future[Seq[User]] =
    db.run(q.result).map(_.map(mapper.fromRow))

  override def updateUser(user: User): Future[Try[Int]] = {
    val updatedRow = mapper.fromDomain(user)
    val updatedField = q.filter(_.id === user.id).update(updatedRow)

    db.run(updatedField.asTry)
  }
  override def deleteUser(id: UUID): Future[Try[Int]] = {
    val deletedRow = q.filter(_.id === id)
      .map(_.status)
      .update(UserStatus.Deleted)

    db.run(deletedRow.asTry)
  }

}
