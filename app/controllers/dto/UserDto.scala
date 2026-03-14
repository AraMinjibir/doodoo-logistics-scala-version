package controllers.dto

import domain.errors.{DomainError, ValidationError}
import domain.models.{User, UserRole, UserStatus}
import play.api.libs.json.{Format, Json, OFormat}

import java.time.Instant
import java.util.UUID

private[controllers] case class SignUpDto(
                                      name: String,
                                      email: String,
                                      password: String,
                                      phone: String,
                                      role: UserRole
                                    ) {
  def toSignUpDomain:Either[DomainError, User] = User.createUser(
    name = name,
    email = email,
    password = password,
    phone = phone,
    role = role
  ).left.map(errors => ValidationError(errors.mkString(", ")))
}
private[controllers] case class LoginDto(
                                     email: String,
                                     hashPassword: String,
                                   )
private[controllers] case class UserResponseDto (
                                             id: UUID,
                                             name: String,
                                             email: String,
                                             phone: String,
                                             role: UserRole,
                                             status: UserStatus,
                                             createdAt: Instant,
                                             updatedAt: Option[Instant],
                                             token: String

                                          )
object UserResponseDto {
  implicit val userStatusFormat: Format[UserStatus] =
    controllers.json.UserStatusJson.format

  implicit val userRoleFormat: Format[UserRole] =
    controllers.json.UserRoleJson.format
  implicit val format:OFormat[UserResponseDto] = Json.format[UserResponseDto]

  def toUserResponseDto(domain: User, token:String): UserResponseDto = {
    UserResponseDto(
      id = domain.id,
      name = domain.name,
      email = domain.email,
      phone = domain.phone,
      role = domain.role,
      status = domain.status,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      token = token
    )
  }
}

object SignUpDto {
  implicit val userStatusFormat: Format[UserStatus] =
    controllers.json.UserStatusJson.format

  implicit val userRoleFormat: Format[UserRole] =
    controllers.json.UserRoleJson.format

  implicit val format:OFormat[SignUpDto] = Json.format[SignUpDto]

  def toUserDomain(dto:SignUpDto):User = {

    User.createUser(
      name = dto.name,
      email = dto.email,
      password = dto.password,
      phone = dto.phone,
      role = dto.role
    ).fold(
      errors => throw new RuntimeException(errors.mkString(",")),
      identity
    )
  }
}
object LoginDto {
  implicit val format: OFormat[LoginDto] = Json.format[LoginDto]
}