package domain.validation.impl

import api.dto.{UsersCreationDto, UsersUpdateDto}
import domain.models.errors.DomainError
import domain.validation.UserValidation
import play.api.data.validation.ValidationError

class UserValidationImpl extends UserValidation{
  private def fail(msg: String): Either[DomainError, Unit] = {
    // Explicitly calling the constructor with the parent type
    Left[DomainError, Unit](DomainError.ValidationError(msg))
  }

  override def validateUserCreation(dto: UsersCreationDto): Either[DomainError, Unit] = {
    if (dto.name.trim.isEmpty)                        return fail("Name cannot be empty")
    if (dto.email.trim.isEmpty || !dto.email.contains("@")) return fail("Invalid email address")
    if (dto.phoneNumber.trim.length < 10)            return fail("Phone number must be at least 10 digits")
    if (dto.hashPassword.trim.length < 6)            return fail("Password must be at least 6 characters")
    if (dto.role == null)                             return fail("User role is required")

    Right(())
  }
  override def validateUserUpdate(dto: UsersUpdateDto): Either[DomainError, Unit] = {
    if (dto.name.trim.isEmpty) fail("Name cannot be empty")
    else if (dto.email.trim.isEmpty || !dto.email.contains("@")) fail("Invalid email address")
    else if (dto.phoneNumber.trim.length < 10) fail("Phone number must be at least 10 digits")
    else if (dto.role == null) fail("User role is required")
    else Right(())
  }


}
