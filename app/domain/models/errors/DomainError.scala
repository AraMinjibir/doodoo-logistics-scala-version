package domain.models.errors

sealed trait DomainError {
  def message: String
}

object DomainError {
  case object UserNotFound extends DomainError {
    val message = "The requested user does not exist."
  }
  case object EmailAlreadyTaken extends DomainError {
    val message = "This email is already registered in our system."
  }
  case class DatabaseError(cause: String) extends DomainError {
    val message = s"An internal database error occurred: $cause"
  }
  case class ValidationError(message: String) extends DomainError
  case class GenericError(message: String) extends DomainError
}