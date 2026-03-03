package domain.models

import play.api.mvc.PathBindable


sealed trait ComplaintStatus

object ComplaintStatus {
  case object Open extends ComplaintStatus
  case object InProgress extends ComplaintStatus
  case object Resolved extends ComplaintStatus
  case object Cancelled extends ComplaintStatus


  val values: Seq[ComplaintStatus] =
    Seq(Open,InProgress, Resolved)


  def fromString(value: String): Option[ComplaintStatus] =
    values.find(toString(_) == value)

  def toString(status: ComplaintStatus): String =
    status match {
      case Open           => "Open"
      case InProgress     => "InProgress"
      case Resolved       => "Resolved"
      case Cancelled      => "Cancelled"
    }

  //  Path bindable for type-safe routing
  implicit def pathBindable(implicit stringBinder: PathBindable[String]): PathBindable[ComplaintStatus] =
    new PathBindable[ComplaintStatus] {
      override def bind(key: String, value: String): Either[String, ComplaintStatus] = {
        fromString(value).toRight(s"Status '$value' is not a valid ComplaintStatus")
      }

      override def unbind(key: String, value: ComplaintStatus): String = value.toString
    }
}

