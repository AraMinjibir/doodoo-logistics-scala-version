package controllers

import api.dto.UsersCreationDto
import com.google.inject.{Inject, Singleton}
import domain.services.UserService
import domain.services.impl.UserServiceImpl
import mappers.UserMapper
import play.api.libs.json._
import play.api.mvc._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()( userService: UserService,
                                cc: ControllerComponents)(implicit ec:ExecutionContext)
                                extends AbstractController(cc){

  def createUser:Action[JsValue] = Action.async(parse.json){ request =>
    request.body.validate[UsersCreationDto].fold(
      errors => Future.successful(BadRequest(Json.obj(
        "status" -> "Errors",
        "message" -> "Validation Failed",
      "error" -> JsError.toJson(errors)))
      ),

     userData => {
       userService.createUser(userData).map{
         case Right(user) => Created(Json.toJson(user))
         case Left(errorMessage) => Conflict(Json.obj("message" -> errorMessage))
       }.recover{
         case e: IllegalArgumentException => BadRequest(Json.obj("error" -> e.getMessage))
         case _ => InternalServerError(Json.obj("error" -> "Unexpected error"))
       }
     }
    )

  }
  def findUserById(userId:UUID): Action[AnyContent] = Action.async{ implicit request =>
    userService.getUserById(userId).map{
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound
    }
  }
  def findUserByEmail(email:String): Action[AnyContent] = Action.async{implicit request =>
    userService.getUserByEmail(email).map{
      case Some(userWithEmail) => Ok(Json.toJson(userWithEmail))
      case None => NotFound
    }
  }

}
