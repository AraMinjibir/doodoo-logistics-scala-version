package domain.validation

import controllers.dto.{UsersCreationDto, UsersUpdateDto}
import domain.models.User
import domain.models.errors.DomainError

trait UserValidation {

  def validateUserCreation(user: User): Either[DomainError, Unit]
  def validateUserUpdate(user: User): Either[DomainError, Unit]

}
