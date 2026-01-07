package mappers

import com.google.inject.Singleton
import domain.models.User
import infrastructure.persistence.models.UsersRow

@Singleton
class UserRowMapper {

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
