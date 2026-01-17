package domain.validation.impl

import controllers.dto.{UsersCreationDto, UsersUpdateDto}
import domain.errors.DomainError
import domain.models.User
import domain.validation.UserValidation
import play.api.data.validation.ValidationError

class UserValidationImpl extends UserValidation{
  private def fail(msg: String): Either[DomainError, Unit] = {
    // Explicitly calling the constructor with the parent type
    Left[DomainError, Unit](DomainError.ValidationError(msg))
  }

  override def validateUserCreation(user:User): Either[DomainError, Unit] = {
    if( user.name.trim.isEmpty)                        return fail("Name cannot be empty")
    if (user.email.trim.isEmpty || !user.email.contains("@")) return fail("Invalid email address")
    if (user.phoneNumber.trim.length < 10)            return fail("Phone number must be at least 10 digits")
    if (user.hashPassword.trim.length < 6)            return fail("Password must be at least 6 characters")
    if (user.role == null)                             return fail("User role is required")

    Right(())
  }
  override def validateUserUpdate(user: User): Either[DomainError, Unit] = {
    if (user.name.trim.isEmpty) fail("Name cannot be empty")
    else if (user.email.trim.isEmpty || !user.email.contains("@")) fail("Invalid email address")
    else if (user.phoneNumber.trim.length < 10) fail("Phone number must be at least 10 digits")
    else if (user.role == null) fail("User role is required")
    else Right(())
  }


}
