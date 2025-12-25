package domain.models

import play.api.libs.json.{Json, OFormat}

import java.util.UUID

case class User(
                id:UUID,
                name: String,
                email: String,
                hashPassword: String,
                phoneNumber: String,
                role:UsersRole
                )
object User {
  implicit val format: OFormat[User] = Json.format[User]
}