package controllers.helpers

import domain.errors.{DomainError, _}
import domain.models.ComplaintStatus
import play.api.Logger
import play.api.libs.json.{JsError, Json}
import play.api.mvc.Results._
import play.api.mvc.Result

import java.sql.SQLException
import scala.util.control.NonFatal

trait ResultMapper {

  private val logger = Logger(classOf[ResultMapper])
  /**
   * Maps a DomainError to a standard Play Result
   */
  def toResult(error: DomainError): Result = error match {
    case DatabaseError(c) => InternalServerError(Json.obj("error" -> s"A database error occurred: $c"))
    case DuplicateEntity => BadRequest(Json.obj())
    case ForeignKeyViolation => BadRequest(Json.obj())
    case NullConstraintViolation => BadRequest(Json.obj())
    case CheckConstraintViolation => BadRequest(Json.obj())
    case DataTooLong => BadRequest(Json.obj())
    case InvalidDataFormat => BadRequest(Json.obj())
    case NumericOverflow => BadRequest(Json.obj())
    case DeadlockDetected => BadRequest(Json.obj())
    case TransactionTimeout => BadRequest(Json.obj())
    case SerializationFailure => BadRequest(Json.obj())
    case GenericError(m)  => BadRequest(Json.obj("error" -> m))
    case ValidationError(msg) => BadRequest(Json.obj("error" -> msg))
    case ShipmentCreationError(msg) => BadRequest(Json.obj("error" -> msg))
    case UpdateShipmentStatusError(msg) => BadRequest(Json.obj("error" -> msg))
    case ComplaintCreationError(msg) => BadRequest(Json.obj("error" -> msg))
    case InvalidComplaintState(from, to) =>
      Conflict(
        Json.obj(
          "code"    -> "INVALID_COMPLAINT_STATE",
          "message" -> error.message,
          "from"    -> from.toString,
          "to"      -> to.toString
        )
      )  }

  /**
   * Standardizes JSON validation error responses
   */
  def onValidationError(errors: scala.collection.Seq[(play.api.libs.json.JsPath, scala.collection.
  Seq[play.api.libs.json.JsonValidationError])]): Result = {
    BadRequest(Json.obj(
      "status"  -> "Error",
      "code"    -> 400,
      "message" -> "The data provided is invalid",
      "details" -> JsError.toJson(errors)
    ))
  }

  def handleException(e: Throwable): Result = {
    logger.error("Unexpected error occurred", e)
    InternalServerError(Json.obj(
      "error" -> "InternalServerError",
      "message" -> "An unexpected error occurred. Please contact support."
    ))
  }

  def mapInsertException(ex: Throwable): DomainError = ex match {

    // Constraint violations

    // Unique constraint (duplicate key)
    case ex: SQLException if isUniqueViolation(ex) =>
      DuplicateEntity

    // Foreign key violation
    case ex: SQLException if isForeignKeyViolation(ex) =>
      ForeignKeyViolation

    // NOT NULL constraint
    case ex: SQLException if isNotNullViolation(ex) =>
      NullConstraintViolation

    // CHECK constraint
    case ex: SQLException if isCheckViolation(ex) =>
      CheckConstraintViolation

    // Data issues
    case ex: SQLException if isDataTooLong(ex) =>
      DataTooLong

    case ex: SQLException if isInvalidFormat(ex) =>
      InvalidDataFormat

    case ex: SQLException if isNumericOverflow(ex) =>
      NumericOverflow


    // Transaction issues
    case ex: SQLException if isDeadlock(ex) =>
      DeadlockDetected

    case ex: SQLException if isTimeout(ex) =>
      TransactionTimeout

    case ex: SQLException if isSerializationFailure(ex) =>
      SerializationFailure


    // Slick / generic
    case NonFatal(e) =>
      DatabaseError(e.getMessage)
  }

  //helpers (SQLState-based)

  private def isUniqueViolation(ex: SQLException): Boolean =
    ex.getSQLState == "23505"

  private def isForeignKeyViolation(ex: SQLException): Boolean =
    ex.getSQLState == "23503"

  private def isNotNullViolation(ex: SQLException): Boolean =
    ex.getSQLState == "23502"

  private def isCheckViolation(ex: SQLException): Boolean =
    ex.getSQLState == "23514"

  private def isDataTooLong(ex: SQLException): Boolean =
    ex.getSQLState == "22001"

  private def isInvalidFormat(ex: SQLException): Boolean =
    ex.getSQLState.startsWith("22")

  private def isNumericOverflow(ex: SQLException): Boolean =
    ex.getSQLState == "22003"

  private def isDeadlock(ex: SQLException): Boolean =
    ex.getSQLState == "40P01"

  private def isTimeout(ex: SQLException): Boolean =
    ex.getSQLState == "57014"

  private def isSerializationFailure(ex: SQLException): Boolean =
    ex.getSQLState == "40001"
}