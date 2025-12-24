package domain.validation.impl

import api.dto.{UsersCreationDto, UsersUpdateDto}
import domain.validation.UserValidation

class UserValidationImpl extends UserValidation{
  override def validateUserCreation(dto: UsersCreationDto): Either[String, Unit] = {
    if (dto.name.trim.isEmpty) Left("Name cannot be empty")
    else if (dto.email.trim.isEmpty || !dto.email.contains("@")) Left("Invalid email address")
    else if (dto.phoneNumber.trim.length < 10) Left("Phone number must be at least 10 digits")
    else if (dto.hashPassword.trim.length < 6) Left("Password must be at least 6 characters")
    else if (dto.role == null)
      Left("User role is required")
    else Right(())
  }

  override def validateUserUpdate(dto: UsersUpdateDto): Either[String, Unit] = {
    if (dto.name.trim.isEmpty) Left("Name cannot be empty")
    else if (dto.email.trim.isEmpty || !dto.email.contains("@")) Left("Invalid email address")
    else if (dto.phoneNumber.trim.length < 10) Left("Phone number must be at least 10 digits")
    else if (dto.role == null)
      Left("User role is required")
    else Right(())
  }


}
