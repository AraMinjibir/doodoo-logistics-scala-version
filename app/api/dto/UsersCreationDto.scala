package api.dto

import domain.models.UsersRole
import play.api.libs.json.{Json, OFormat}

case class UsersCreationDto(
                             name: String,
                             email: String,
                             hashPassword: String,
                             phoneNumber: String,
                             role:UsersRole
                           )

object UsersCreationDto {
  implicit val format: OFormat[UsersCreationDto] = Json.format[UsersCreationDto]
}