package controllers

import com.google.inject.{Inject, Singleton}
import controllers.action.AuthAction
import controllers.helpers.ResultMapper
import controllers.dto.{LoginDto, SignUpDto, UserResponseDto}
import domain.errors.UserAlreadyExists
import domain.models.UserRole.Admin
import domain.models.{UserRole, UserStatus}
import domain.services.UserService
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(
                              userService: UserService,
                              authenticatedAction: AuthAction,
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
          case Right(_) =>

            val user = SignUpDto.toUserDomain(dto)

            userService.registerUser(user).flatMap {
              case Left(UserAlreadyExists(email)) =>
                Future.successful(
                  Conflict(Json.obj("message" -> s"User with email $email already exists"))
                )

              case Right(createdUser) =>
                userService.login(createdUser.email, dto.password).map {
                  case Left(_) =>
                    InternalServerError("User created but login failed")

                  case Right(token) =>
                    Created(Json.toJson(
                      UserResponseDto.toUserResponseDto(createdUser, token)
                    ))
                }
            }.recover{
              case ex =>
                handleException(ex)
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
            }.recover{
              case ex =>
                handleException(ex)
            }
        }
    )
  }

  def listAllUsers: Action[AnyContent] = authenticatedAction.withRole(Set(Admin)).async {
    userService.listAllUsers.map { users =>
      Ok(Json.toJson(users.map(user => UserResponseDto.toUserResponseDto(user, ""))))
    }.recover{
      case ex =>
        handleException(ex)
    }
  }

  def getUserById(userId:UUID): Action[AnyContent] = authenticatedAction.withRole(Set(Admin))
    .async {
    userService.findUserById(userId).map {
      case Some(user) => Ok(Json.toJson(UserResponseDto.toUserResponseDto(user, "")))
      case None       => NotFound(Json.obj("error" -> s"User not found with ID $userId"))
    }.recover{
      case ex =>
        handleException(ex)
    }
  }
  def getUserByUsername(username: String): Action[AnyContent] = authenticatedAction.withRole(Set(Admin))
    .async{
    userService.findUserByUsername(username).map{
      case Some(user) => Ok(Json.toJson(UserResponseDto.toUserResponseDto(user, "")))
      case None => NotFound(Json.obj("error" -> s"No user found with this username: $username"))
    }.recover{
      case ex =>
        handleException(ex)
    }
  }
  def getUserByRole(role: UserRole): Action[AnyContent] = authenticatedAction.withRole(Set(Admin))
    .async{
    userService.findUserByRole(role).map{users =>
      Ok(Json.toJson(users.map(user => UserResponseDto.toUserResponseDto(user, ""))))

    }.recover{
      case ex =>
        handleException(ex)
    }
  }
  def getUserByStatus(status:UserStatus): Action[AnyContent] = authenticatedAction.withRole(Set(Admin))
    .async {
    userService.findUserByStatus(status).map { users =>
      Ok(Json.toJson(users.map(user => UserResponseDto.toUserResponseDto(user, ""))))
    }
  }
  def updateUser(id: UUID): Action[JsValue] = authenticatedAction.withRole(Set(Admin))
    .async(parse.json) { implicit request =>
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

    ).recover{
      case ex =>
        handleException(ex)
    }
  }

  def updateStatus(userId:UUID, status: UserStatus): Action[AnyContent] = authenticatedAction.withRole(Set(Admin))
    .async {
    userService.updateUserStatus(userId, status).map {
      case Left(err)        => toResult(err)
      case Right(updatedUser) =>
        Ok(Json.toJson(UserResponseDto.toUserResponseDto(updatedUser, "")))
    }.recover{
      case ex =>
        handleException(ex)
    }
  }

  def deleteUser(userId:UUID): Action[AnyContent] = authenticatedAction.withRole(Set(Admin))
    .async {

    userService.deleteUser(userId).map {
      case Left(err) => toResult(err)
      case Right(_)  => NoContent
    }.recover{
      case ex =>
        handleException(ex)
    }
  }



}
