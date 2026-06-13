package domain.models

import jdk.internal.net.http.frame.Http2Frame.asString
import play.api.mvc.PathBindable


sealed trait PaymentMethod
object PaymentMethod {
  case object Card extends PaymentMethod
  case object BankTransfer extends PaymentMethod
  case object MobileMoney extends PaymentMethod


  val values: Seq[PaymentMethod] =
    Seq(Card,BankTransfer, MobileMoney)


  def fromString(value: String): Option[PaymentMethod] =
    values.find(pm => toString(pm).equalsIgnoreCase(value))

  def toString(status: PaymentMethod): String =
    status match {
      case Card           => "Card"
      case BankTransfer     => "BankTransfer"
      case MobileMoney       => "MobileMoney"
    }

  //  Path bindable for type-safe routing
  implicit def pathBindable(
                             implicit stringBinder: PathBindable[String]
                           ): PathBindable[PaymentMethod] =
    new PathBindable[PaymentMethod] {

      override def bind(
                         key: String,
                         value: String
                       ): Either[String, PaymentMethod] =
        fromString(value)
          .toRight(s"'$value' is not a valid PaymentMethod")

      override def unbind(
                           key: String,
                           value: PaymentMethod
                         ): String =
        PaymentMethod.toString(value)
    }
}

sealed trait PaymentStatus
object PaymentStatus {
  case object Pending extends PaymentStatus
  case object Successful extends PaymentStatus
  case object Failed extends PaymentStatus
  case object Refunded extends PaymentStatus


  val values: Seq[PaymentStatus] =
    Seq(Pending,Successful, Failed, Refunded)


  def fromString(value: String): Option[PaymentStatus] =
    values.find(pm => toString(pm).equalsIgnoreCase(value))

  def toString(status: PaymentStatus): String =
    status match {
      case Pending           => "Pending"
      case Successful     => "Successful"
      case Failed       => "Failed"
      case Refunded => "Refunded"
    }

  //  Path bindable for type-safe routing
  implicit def pathBindable(
                             implicit stringBinder: PathBindable[String]
                           ): PathBindable[PaymentStatus] =
    new PathBindable[PaymentStatus] {

      override def bind(
                         key: String,
                         value: String
                       ): Either[String, PaymentStatus] =
        PaymentStatus.fromString(value)
          .toRight(s"'$value' is not a valid PaymentStatus")

      override def unbind(
                           key: String,
                           value: PaymentStatus
                         ): String =
        PaymentStatus.toString(value)
    }
}
