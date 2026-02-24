package domain.models


sealed trait ComplaintStatus

object ComplaintStatus {
  case object Open extends ComplaintStatus
  case object InProgress extends ComplaintStatus
  case object Resolved extends ComplaintStatus
  case object Cancelled extends ComplaintStatus


  val values: Seq[ComplaintStatus] =
    Seq(Open,InProgress, Resolved)
  private val validTransitions: Map[ComplaintStatus, Set[ComplaintStatus]] = Map(
    ComplaintStatus.Open       -> Set(ComplaintStatus.InProgress, ComplaintStatus.Cancelled),
    ComplaintStatus.InProgress      -> Set(ComplaintStatus.Resolved, ComplaintStatus.Cancelled),
    ComplaintStatus.Resolved      -> Set.empty,
    ComplaintStatus.Cancelled      -> Set.empty
  )

  def fromString(value: String): Option[ComplaintStatus] =
    values.find(toString(_) == value)

  def toString(status: ComplaintStatus): String =
    status match {
      case Open           => "Open"
      case InProgress     => "InProgress"
      case Resolved       => "Resolved"
      case Cancelled      => "Cancelled"
    }
}

