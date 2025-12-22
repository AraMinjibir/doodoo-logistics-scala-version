package infrastructure.persistence.tables

import domain.models.UsersRole
import infrastructure.persistence.models.UsersRow
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

class UsersTable(tag: Tag) extends Table[UsersRow](tag,"users"){


  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def email = column[String]("email_address", O.Unique, O.Length(254))
  def hashPassword = column[String]("hash_password")
  def phoneNumber = column[String]("phone_number")
  def role = column[UsersRole]("role")

   def * : ProvenShape[UsersRow] = (id,name, email,hashPassword, phoneNumber, role) <> (UsersRow.tupled , UsersRow.unapply)


}

object UsersTable {
  val table = TableQuery[UsersTable]
}

