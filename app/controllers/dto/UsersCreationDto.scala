package controllers.dto

import domain.models.{User, UserUpdateData, UsersRole}
import play.api.libs.json.{Format, Json, OFormat}

import java.util.UUID

private[controllers] case class UsersCreationDto(
                             name: String,
                             email: String,
                             hashPassword: String,
                             phoneNumber: String,
                             role:UsersRole
                           )
private[controllers]  case class UsersUpdateDto(
                             name: String,
                             email: String,
                             phoneNumber: String,
                             role:UsersRole
                           )

object UsersCreationDto {
  implicit val usersRoleFormat: Format[UsersRole] =
    controllers.json.UsersRoleJson.format

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
  def toDto(domain:User):UserResponseDto = {
    UserResponseDto(
      id = domain.id,
      name = domain.name,
      email = domain.email,
      phoneNumber = domain.phoneNumber,
      role = domain.role
    )
  }

  def applyUpdate(dto: UsersUpdateDto): UserUpdateData =
    UserUpdateData(
      name = dto.name,
      email = dto.email,
      phoneNumber = dto.phoneNumber,
      role = dto.role
    )




  implicit val format: OFormat[UsersCreationDto] = Json.format[UsersCreationDto]
}
object UsersUpdateDto {
  implicit val usersRoleFormat: Format[UsersRole] =
    controllers.json.UsersRoleJson.format
  implicit val format: OFormat[UsersUpdateDto] = Json.format[UsersUpdateDto]
}