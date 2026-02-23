package domain.errors

import domain.models.ShipmentStatus

import java.util.UUID

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
case object ProofMustContainImageOrNote extends DomainError {
  val message = "Proof of delivery must contain either an image or a note."
}

case object SubmittedByEmpty extends DomainError {
  val message = "submittedBy must not be empty."
}

case object ShipmentNotDelivered extends DomainError {
  val message = "Shipment is not delivered yet."
}

case object DuplicateProofOfDelivery extends DomainError {
  val message = "Duplicate proof detected."
}

//Generic failure

case class DatabaseError(cause: String) extends DomainError {
  val message = s"Database error occurred: $cause"
}

case class ShipmentCreationError(cause: String) extends DomainError {
  val message = s"Unable to create the shipment: $cause"
}

case class UpdateShipmentStatusError(cause: String) extends DomainError {
  val message = s"Unable to update the shipment: $cause"
}
final case class ShipmentNotFound(trackingNumber: String) extends DomainError {
  val message = s"Shipment $trackingNumber not found"
}
final case class ShipmentNotFoundById(id: UUID) extends DomainError {
  val message = s"Shipment with id: $id not found"
}

final case class InvalidShipmentStatusTransition(
                                                  from: ShipmentStatus,
                                                  to: ShipmentStatus
                                                ) extends DomainError {
  val message =
    s"Invalid shipment status transition from ${ShipmentStatus.toString(from)} to ${ShipmentStatus.toString(to)}"
}

case class UpdateProofOfDeliveryError(cause: String) extends DomainError {
  val message = s"Unable to update the proof of delivery: $cause"
}

  case class ValidationError(message: String) extends DomainError
  case class GenericError(message: String) extends DomainError

