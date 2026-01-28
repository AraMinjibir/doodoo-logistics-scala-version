package domain.errors

sealed trait DomainError {
  def message: String
}

//Constraint violation
case object DuplicateEntity extends DomainError {
  val message = "An entity with the same unique identifier already exists."
}

case object ForeignKeyViolation extends DomainError {
  val message = "A referenced entity does not exist."
}

case object NullConstraintViolation extends DomainError {
  val message = "A required field was missing."
}

case object CheckConstraintViolation extends DomainError {
  val message = "One or more fields violated a domain constraint."
}

//Data type
case object DataTooLong extends DomainError {
  val message = "One or more fields exceeded the allowed length."
}

case object InvalidDataFormat extends DomainError {
  val message = "Invalid data format was provided."
}

case object NumericOverflow extends DomainError {
  val message = "A numeric value exceeded the allowed range."
}

// Transactions errors

case object DeadlockDetected extends DomainError {
  val message = "The operation failed due to a database deadlock."
}

case object TransactionTimeout extends DomainError {
  val message = "The database transaction timed out."
}

case object SerializationFailure extends DomainError {
  val message = "The transaction could not be serialized due to concurrent access."
}

//Generic failure

case class DatabaseError(cause: String) extends DomainError {
  val message = s"Database error occurred: $cause"
}

case class ShipmentCreationError(cause: String) extends DomainError {
  val message = s"Unable to create the shipment: $cause"
}

  case class ValidationError(message: String) extends DomainError
  case class GenericError(message: String) extends DomainError

