package controllers

import com.google.inject.{Inject, Singleton}
import controllers.dto.{UserResponseDto, UsersCreationDto, UsersUpdateDto}
import controllers.helpers.ResultMapper
import domain.models.UsersRole
import domain.services.UserService
import play.api.libs.json._
import play.api.mvc._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()( userService: UserService,
                                cc: ControllerComponents)(implicit ec:ExecutionContext)
                                extends AbstractController(cc) with ResultMapper{

  def createUser: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[UsersCreationDto].fold(
      errors   => Future.successful(onValidationError(errors)),
      userDto => userService.createUser(UsersCreationDto.toDomain(userDto)).map {
        case Right(user) => Created(Json.toJson(UsersCreationDto.toDto(user)))
        case Left(error) => toResult(error)
      }
    ).recover { case e => handleException(e) }
  }
  def findUserById(userId:UUID): Action[AnyContent] = Action.async { implicit request =>
    userService.getUserById(userId).map {
      case Some(user) => Ok(Json.toJson(UserResponseDto.fromDomain(user)))
      case None => NotFound(Json.obj("message" -> s"No user found with this id: $userId"))
    }.recover { case e => handleException(e) }
  }
  def findUserByEmail(email:String): Action[AnyContent] = Action.async{implicit request =>
    userService.getUserByEmail(email).map{
      case Some(user) => Ok(Json.toJson(UserResponseDto.fromDomain(user)))
      case None => NotFound(Json.obj("message" -> s"No user found with this email addres: $email"))
    }.recover { case e => handleException(e) }
  }
  def findUserByRole(role:UsersRole): Action[AnyContent] = Action.async{ implicit request =>
    userService.getUserByRole(role).map{users =>
      Ok(Json.toJson(users.map(UserResponseDto.fromDomain)))
    }.recover { case e => handleException(e) }
  }
  def findAllUsers(offset: Int, limit: Int): Action[AnyContent] = Action.async{ implicit  request =>
    userService.getAllUsers(offset, limit).map{allUser =>
      Ok(Json.toJson(allUser.map(UserResponseDto.fromDomain)))
    }.recover { case e => handleException(e) }
  }
  def updateUserDetails(userId:UUID): Action[JsValue] = Action.async(parse.json){ request =>
    request.body.validate[UsersUpdateDto].fold(
      errors => Future.successful(onValidationError(errors)),
      userDetails => {
        val updatedDetails = UsersCreationDto.applyUpdate(userDetails)
        userService.updateUser(userId, updatedDetails).map{
          case Right(updatedUser) => Ok(Json.toJson(UserResponseDto.fromDomain(updatedUser)))
          case Left(error) => toResult(error)
        }
      }
    ).recover { case e => handleException(e) }
  }
  def deleteUser(userId:UUID): Action[AnyContent] = Action.async{implicit request =>
    userService.deleteUser(userId).map{
      case Right(deletedUser) => Ok(Json.toJson(deletedUser))
      case Left(error) => toResult(error)
    }.recover { case e => handleException(e) }
  }

}
