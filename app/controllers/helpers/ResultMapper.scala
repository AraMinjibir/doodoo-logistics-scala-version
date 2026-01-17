package controllers.helpers

import domain.errors.DomainError
import domain.errors._
import play.api.Logger
import play.api.libs.json.{JsError, Json}
import play.api.mvc.Results._
import play.api.mvc.Result

trait ResultMapper {

  private val logger = Logger(classOf[ResultMapper])
  /**
   * Maps a DomainError to a standard Play Result
   */
  def toResult(error: DomainError): Result = error match {
    case UserNotFound              => NotFound(Json.obj("error" -> error.message))
    case EmailAlreadyTaken         => Conflict(Json.obj("error" -> error.message))
    case DatabaseError(c) => InternalServerError(Json.obj("error" -> "A database error occurred"))
    case GenericError(m)  => BadRequest(Json.obj("error" -> m))
    case ValidationError(msg) => BadRequest(Json.obj("error" -> msg))
  }

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
}