package domain.validation

import api.dto.{UsersCreationDto, UsersUpdateDto}

trait UserValidation {

  def validateUserCreation(dto: UsersCreationDto): Either[String, Unit]
  def validateUserUpdate(dto: UsersUpdateDto): Either[String, Unit]

}
