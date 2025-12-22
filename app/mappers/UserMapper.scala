package mappers

import api.dto.{UsersCreationDto, UsersCreationResponse}
import domain.models.User
import infrastructure.persistence.models.UsersRow

import java.util.UUID

class UserMapper {

//  Converting from UsersCreationDto to Domin
  def toDomain(dto:UsersCreationDto):User = {
    User(
      id = UUID.randomUUID(),
      name = dto.name,
      email = dto.email,
      hashPassword =  dto.hashPassword,
      phoneNumber =  dto.phoneNumber,
      role =  dto.role
    )
  }

//  Converting from Domain model to Users Creation Response

  def toDto(domain:User):UsersCreationResponse = {
    UsersCreationResponse(
      id = domain.id,
      name = domain.name,
      email = domain.email,
      phoneNumber = domain.phoneNumber,
      role = domain.role
    )
  }

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
