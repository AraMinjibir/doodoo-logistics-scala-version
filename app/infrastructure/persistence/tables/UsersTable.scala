package infrastructure.persistence.tables

import domain.models.UsersRole
import infrastructure.persistence.models.UsersRow
import play.api.libs.json.Format
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

class UsersTable(tag: Tag) extends Table[UsersRow](tag,"users"){

  import UsersTable._

  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def email = column[String]("email_address", O.Unique, O.Length(254))
  def hashPassword = column[String]("hash_password")
  def phoneNumber = column[String]("phone_number")
  def role = column[UsersRole]("role")

   def * : ProvenShape[UsersRow] = (id,name, email,hashPassword, phoneNumber, role) <> (UsersRow.tupled , UsersRow.unapply)


}

object UsersTable {

  implicit val usersRoleFormat: Format[UsersRole] =
    controllers.json.UsersRoleJson.format

  implicit val roleColumnType: JdbcType[UsersRole] with BaseTypedType[UsersRole] =
    MappedColumnType.base[UsersRole, String](
      _.toString,
      s =>
        UsersRole.fromString(s).getOrElse(
          throw new IllegalArgumentException(s"Database contains invalid role: $s")
        )
    )

  val table = TableQuery[UsersTable]
}


