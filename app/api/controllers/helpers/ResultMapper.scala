package api.controllers.helpers

import domain.models.errors.DomainError.{EmailAlreadyTaken, UserNotFound}
import domain.models.errors._
import play.api.libs.json.{JsError, Json}
import play.api.mvc.Results._
import play.api.mvc.Result

trait ResultMapper {

  /**
   * Maps a DomainError to a standard Play Result
   */
  def toResult(error: DomainError): Result = error match {
    case UserNotFound              => NotFound(Json.obj("error" -> error.message))
    case EmailAlreadyTaken         => Conflict(Json.obj("error" -> error.message))
    case DomainError.DatabaseError(c) => InternalServerError(Json.obj("error" -> "A database error occurred"))
    case DomainError.GenericError(m)  => BadRequest(Json.obj("error" -> m))
  }

  /**
   * Standardizes JSON validation error responses
   */
  def onValidationError(errors: scala.collection.Seq[(play.api.libs.json.JsPath, scala.collection.Seq[play.api.libs.json.JsonValidationError])]): Result = {
    BadRequest(Json.obj(
      "status"  -> "Error",
      "code"    -> 400,
      "message" -> "The data provided is invalid",
      "details" -> JsError.toJson(errors)
    ))
  }
}