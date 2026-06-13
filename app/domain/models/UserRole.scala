package domain.models

import play.api.mvc.PathBindable

sealed trait UserRole

object UserRole {
  case object Admin extends UserRole
  case object Sender extends UserRole
  case object Recipient extends UserRole
  case object ServiceProvider extends UserRole
  case object CustomerSupportAgent extends UserRole


  val values: Seq[UserRole] =
    Seq(Admin,Sender, Recipient, ServiceProvider,CustomerSupportAgent)


  def fromString(value: String): Option[UserRole] =
    values.find(pm => toString(pm).equalsIgnoreCase(value))

  def toString(status: UserRole): String =
    status match {
      case Admin           => "Admin"
      case Sender     => "Sender"
      case Recipient       => "Recipient"
      case ServiceProvider => "ServiceProvider"
      case CustomerSupportAgent => "CustomerSupportAgent"
    }

  //  Path bindable for type-safe routing
  implicit def pathBindable(
                             implicit stringBinder: PathBindable[String]
                           ): PathBindable[UserRole] =
    new PathBindable[UserRole] {

      override def bind(
                         key: String,
                         value: String
                       ): Either[String, UserRole] =
        fromString(value)
          .toRight(s"'$value' is not a valid UserRole")

      override def unbind(
                           key: String,
                           value: UserRole
                         ): String =
        UserRole.toString(value)
    }
}

sealed trait UserStatus

object UserStatus {
  case object Active extends UserStatus
  case object Suspended extends UserStatus
  case object Deleted extends UserStatus

  val values: Seq[UserStatus] =
    Seq(Active,Suspended, Deleted)


  def fromString(value: String): Option[UserStatus] =
    values.find(pm => toString(pm).equalsIgnoreCase(value))

  def toString(status: UserStatus): String =
    status match {
      case Active           => "Active"
      case Suspended     => "Suspended"
      case Deleted       => "Deleted"
    }

  //  Path bindable for type-safe routing
  implicit def pathBindable(
                             implicit stringBinder: PathBindable[String]
                           ): PathBindable[UserStatus] =
    new PathBindable[UserStatus] {

      override def bind(
                         key: String,
                         value: String
                       ): Either[String, UserStatus] =
        fromString(value)
          .toRight(s"'$value' is not a valid UserStatus")

      override def unbind(
                           key: String,
                           value: UserStatus
                         ): String =
        UserStatus.toString(value)
    }
}
