package infrastructure.persistence.models

import domain.models.{User, UsersRole}

import java.util.UUID

case class UsersRow(
                     id:UUID,
                     name: String,
                     email: String,
                     hashPassword: String,
                     phoneNumber: String,
                     role:UsersRole
                   ) {
  def fromRow(row:UsersRow): User = {
    User(
      id = row.id,
      name = row.name,
      email = row.email,
      hashPassword = row.hashPassword,
      phoneNumber = row.phoneNumber,
      role = row.role
    )
  }
  def toRow(row:User): UsersRow = {
    UsersRow(
      id = row.id,
      name = row.name,
      email = row.email,
      hashPassword = row.hashPassword,
      phoneNumber = row.phoneNumber,
      role = row.role
    )
  }
}