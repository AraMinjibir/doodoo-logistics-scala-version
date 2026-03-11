package infrastructure.persistence.tables

import domain.models.{UserRole, UserStatus}
import infrastructure.persistence.models.UserRow
import slick.lifted.{ProvenShape, Tag}
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import java.util.UUID

class UserTable(tag:Tag) extends Table[UserRow](tag, "users"){
  import UserTable._

  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def email = column[String]("username")
  def  hashPassword = column[String]("hash_password")
  def phoneNumber = column[String]("phone_number")
  def role = column[UserRole]("role")
  def  status = column[UserStatus]("status")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * :ProvenShape[UserRow] = (
    id,name,email,hashPassword,phoneNumber,role,status,createdAt,updatedAt
  ) <> ((UserRow.apply _).tupled, UserRow.unapply)

}

object  UserTable {
  val table = TableQuery[UserTable]

  implicit val userRoleColumnType: BaseColumnType[UserRole] =
    MappedColumnType.base[UserRole, String](
      _.toString,
      str =>
        UserRole
          .fromString(str)
          .getOrElse(
            throw new IllegalArgumentException(s"Unknown UserRole: $str")
          )
    )
  implicit val userStatusColumnType: BaseColumnType[UserStatus] =
    MappedColumnType.base[UserStatus, String](
      _.toString,
      str =>
        UserStatus
          .fromString(str)
          .getOrElse(
            throw new IllegalArgumentException(s"Unknown UserStatus: $str")
          )
    )
}
