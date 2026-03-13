package controllers

import com.google.inject.{Inject, Singleton}
import controllers.helpers.ResultMapper
import controllers.dto.{LoginDto, SignUpDto, UserResponseDto}
import domain.errors.{DomainError, InvalidCredentials}
import domain.models.{UserRole, UserStatus}
import domain.services.UserService
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(
                              userService: UserService,
                                cc: ControllerComponents
                              )(implicit ex:ExecutionContext)
                              extends AbstractController(cc)
                              with ResultMapper {


  def signUp: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[SignUpDto].fold(
      errors => Future.successful(onValidationError(errors)),
      dto =>
        dto.toSignUpDomain match {
          case Left(validationError) =>
            Future.successful(toResult(validationError))
          case Right(user) =>

            userService.registerUser(user).flatMap {
              case Left(err) =>
                Future.successful(toResult(err))
              case Right(createdUser) =>
//               Generate token
                userService.login(user.email, dto.password).map {
                  case Left(_) => NotFound("User not found")
                  case Right(token) =>
                    Created(Json.toJson(UserResponseDto.toUserResponseDto(createdUser, token)))
                }
            }
        }
    )
  }

  def login: Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[LoginDto].fold(
      errors => Future.successful(onValidationError(errors)),
      dto =>
        userService.login(dto.email, dto.hashPassword).flatMap {
          case Left(err) =>
            Future.successful(toResult(err))
          case Right(token) =>
            userService.findUserByUsername(dto.email).map {
              case Some(user) =>
                Ok(Json.toJson(UserResponseDto.toUserResponseDto(user, token)))
              case None =>
                NotFound(Json.obj("error" -> s"User not found after login"))
            }
        }
    )
  }

  def listAllUsers: Action[AnyContent] = Action.async {
    userService.listAllUsers.map { users =>
      Ok(Json.toJson(users.map(user => UserResponseDto.toUserResponseDto(user, ""))))
    }
  }

  def getUserById(userId:UUID): Action[AnyContent] = Action.async {
    userService.findUserById(userId).map {
      case Some(user) => Ok(Json.toJson(UserResponseDto.toUserResponseDto(user, "")))
      case None       => NotFound(Json.obj("error" -> s"User not found with ID $userId"))
    }
  }
  def getUserByUsername(username: String): Action[AnyContent] = Action.async{
    userService.findUserByUsername(username).map{
      case Some(user) => Ok(Json.toJson(UserResponseDto.toUserResponseDto(user, "")))
      case None => NotFound(Json.obj("error" -> s"No user found with this username: $username"))
    }
  }
  def getUserByRole(role: UserRole): Action[AnyContent] = Action.async{
    userService.findUserByRole(role).map{users =>
      Ok(Json.toJson(users.map(user => UserResponseDto.toUserResponseDto(user, ""))))

    }
  }
  def getUserByStatus(status:UserStatus): Any = Action.async {
    userService.findUserByStatus(status).map { users =>
      Ok(Json.toJson(users.map(user => UserResponseDto.toUserResponseDto(user, ""))))
    }
  }
  def updateUser(id: UUID): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[SignUpDto].fold(
      errors => Future.successful(onValidationError(errors)),
      dto =>
        dto.toSignUpDomain match {
          case Left(validationError) =>
            Future.successful(BadRequest(Json.obj("error" -> validationError.message)))
          case Right(user) =>
            userService.updateUserDetails(id, user).map {
              case Left(err)  => toResult(err)
              case Right(updatedUser) =>
                Ok(Json.toJson(UserResponseDto.toUserResponseDto(updatedUser, "")))
            }
        }
    )
  }

  def updateStatus(userId:UUID, status: UserStatus): Action[AnyContent] = Action.async {
    userService.updateUserStatus(userId, status).map {
      case Left(err)        => toResult(err)
      case Right(updatedUser) =>
        Ok(Json.toJson(UserResponseDto.toUserResponseDto(updatedUser, "")))
    }
  }

  def deleteUser(userId:UUID): Action[AnyContent] = Action.async {

    userService.deleteUser(userId).map {
      case Left(err) => toResult(err)
      case Right(_)  => NoContent
    }
  }



}
