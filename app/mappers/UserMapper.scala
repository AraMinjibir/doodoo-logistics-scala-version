package mappers

import api.dto.{UsersCreationDto, UsersCreationResponse}
import domain.models.User

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
}
