package domain.models

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}
import play.api.mvc.PathBindable
import slick.jdbc.H2Profile.MappedColumnType
import slick.jdbc.PostgresProfile.api._

sealed trait UsersRole extends Product with Serializable

object UsersRole {
  case object Customer extends UsersRole
  case object ServiceProvider extends UsersRole
  case object SupportAgent extends UsersRole
  case object Admin extends UsersRole

  val values: Seq[UsersRole] = Seq(Customer, ServiceProvider, SupportAgent, Admin)

  def fromString(value:String): Option[UsersRole] = values.find(toString(_) == value)

  def toString(role: UsersRole):String = role.toString

  //  Path bindable for type-safe routing
  implicit def pathBindable(implicit stringBinder: PathBindable[String]): PathBindable[UsersRole] =
    new PathBindable[UsersRole] {
      override def bind(key: String, value: String): Either[String, UsersRole] = {
        fromString(value).toRight(s"Role '$value' is not a valid User role")
      }

      override def unbind(key: String, value: UsersRole): String = value.toString
    }

}
