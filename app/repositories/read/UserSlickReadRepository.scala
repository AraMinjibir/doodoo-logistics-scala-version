package repositories.read

import infrastructure.persistence.tables.UsersTable
import com.google.inject.{Inject, Singleton}
import domain.models.{User, UsersRole}
import mappers.UserMapper
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserSlickReadRepository @Inject()(databaseConfigProvider:
                                        DatabaseConfigProvider, mapper: UserMapper) (implicit ec: ExecutionContext)
  extends UserReadRepository {

  // Initialize the database configuration for the specific JDBC Profile (PostgreSQL)
  val dbConfic = databaseConfigProvider.get[JdbcProfile]

  // Import the profile API to enable Slick DSL
  import dbConfic.profile.api._

  // Database handle to run queries asynchronously
  private val db = dbConfic.db

  // Reference to the table definition (Slick TableQuery)
  private val q = UsersTable.table

//  Fetches a single user by their unique identifier.
//   Maps Future[Option[UsersRow]] to Future[Option[User]]
  override def findUserbyId(id: UUID): Future[Option[User]] ={
    db.run(q.filter(_.id === id).result.headOption).map(_.map(mapper.fromRow))

  }

//  Retrieves a user by their email address.
  override def findUserByEmail(email:String): Future[Option[User]] = {
    db.run(q.filter(_.email === email).result.headOption).map(_.map(mapper.fromRow))
  }

//  Finds all users associated with a specific role
  override def findUserByRole(role: UsersRole): Future[Seq[User]] = {
    db.run(q.filter(_.role === role).result).map(_.map(mapper.fromRow))
  }

  /**
   * Provides a paginated list of users sorted alphabetically by name.
   * @param offset Number of records to skip
   * @param limit Maximum number of records to return
   */
  override def listAllUsers(offset: Int, limit: Int): Future[Seq[User]] = {
    db.run(
      q.sortBy(_.name.asc) // Standardize sorting for consistent UI/UX
        .drop(offset)      // Skip previous records (Pagination)
        .take(limit)      // Limit result set size (Performance)
        .result
    ).map(_.map(mapper.fromRow))
  }

}
