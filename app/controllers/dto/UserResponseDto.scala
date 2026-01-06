package controllers.dto

import domain.models.UsersRole
import play.api.libs.json.{Json, OFormat}

import java.util.UUID

case class UserResponseDto(
                                  id:UUID,
                                 name: String,
                                 email: String,
                                 phoneNumber: String,
                                 role:UsersRole
                               )
object UserResponseDto {

  implicit val format: OFormat[UserResponseDto] = Json.format[UserResponseDto]

    def fromDomain(user: domain.models.User): UserResponseDto = UserResponseDto(
      id = user.id,
      name = user.name,
      email = user.email,
      phoneNumber = user.phoneNumber,
      role = user.role
    )
}