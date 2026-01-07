package controllers.json

import domain.models.UsersRole
import domain.models.UsersRole.fromString
import play.api.libs.json._
import play.api.mvc.PathBindable

object UsersRoleJson {
  implicit val format: Format[UsersRole] = new Format[UsersRole]{
    override def writes(role:UsersRole):JsValue = JsString(UsersRole.toString(role))

    override def reads(json: JsValue): JsResult[UsersRole] = json match {
      case JsString(value) =>
        fromString(value)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Invalid user role: $value"))

      case _ => JsError("User role must be a string")
    }
  }

}
