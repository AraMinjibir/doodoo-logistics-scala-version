package domain.validation

import api.dto.{UsersCreationDto, UsersUpdateDto}
import domain.models.errors.DomainError

trait UserValidation {

  def validateUserCreation(dto: UsersCreationDto): Either[DomainError, Unit]
  def validateUserUpdate(dto: UsersUpdateDto): Either[DomainError, Unit]

}
