package mappers

import com.google.inject.Singleton
import domain.models.User
import infrastructure.persistence.models.UserRow

@Singleton
class UserMapper {

  def fromDomain(domain:User): UserRow = UserRow(
    id = domain.id,
    name = domain.name,
    email = domain.email,
    phone = domain.phone,
    role = domain.role,
    status = domain.status,
    createdAt = domain.createdAt,
    updatedAt = domain.updatedAt
  )

  def fromRow(row:UserRow):User = User(
    id = row.id,
    name = row.name,
    email = row.email,
    phone = row.phone,
    role = row.role,
    status = row.status,
    createdAt = row.createdAt,
    updatedAt = row.updatedAt
  )

}
