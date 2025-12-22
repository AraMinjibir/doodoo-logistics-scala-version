package api.dto

import domain.models.UsersRole
import play.api.libs.json.{Json, OFormat}

import java.util.UUID

case class UsersCreationResponse(
                                  id:UUID,
                                 name: String,
                                 email: String,
                                 phoneNumber: String,
                                 role:UsersRole
                               )
object UsersCreationRespons {
  implicit val format: OFormat[UsersCreationResponse] = Json.format[UsersCreationResponse]
}