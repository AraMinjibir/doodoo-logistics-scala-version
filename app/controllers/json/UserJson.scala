package controllers.json

import domain.models.{UserRole, UserStatus}
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

object UserRoleJson {
  implicit val format: Format[UserRole] = new Format[UserRole] {

    override def writes(status: UserRole): JsValue =
      JsString(UserRole.toString(status))

    override def reads(json: JsValue): JsResult[UserRole] =
      json match {
        case JsString(value) =>
          domain.models.UserRole.fromString(value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Invalid shipment status: $value"))

        case _ => JsError("UserRole must be a string")
      }
  }

}

object UserStatusJson {
  implicit val format: Format[UserStatus] = new Format[UserStatus] {

    override def writes(status: UserStatus): JsValue =
      JsString(UserStatus.toString(status))

    override def reads(json: JsValue): JsResult[UserStatus] =
      json match {
        case JsString(value) =>
          domain.models.UserStatus.fromString(value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Invalid shipment status: $value"))

        case _ => JsError("UserStatus must be a string")
      }
  }

}
