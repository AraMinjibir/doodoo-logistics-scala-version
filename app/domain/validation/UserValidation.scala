package domain.validation

import controllers.dto.{UsersCreationDto, UsersUpdateDto}
import domain.errors.DomainError
import domain.models.User

trait UserValidation {

  def validateUserCreation(user: User): Either[DomainError, Unit]
  def validateUserUpdate(user: User): Either[DomainError, Unit]

}
