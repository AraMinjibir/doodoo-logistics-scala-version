package domain.models

import org.mindrot.jbcrypt.BCrypt

import java.time.Instant
import java.util.UUID


  case class User private(
                   id: UUID,
                   name: String,
                   email: String,
                   hashPassword: String,
                   phone: String,
                   role: UserRole,
                   status: UserStatus,
                   createdAt: Instant,
                   updatedAt: Option[Instant]
                 )


object User {

  def createUser(
                  name: String,
                  email: String,
                  password: String,
                  phone: String,
                  role: UserRole
                ): Either[List[String], User] = {

    val errors = List(
      Option.when(name.trim.isEmpty)("Name must be provided"),
      Option.when(email.trim.isEmpty)("Email must be provided"),
      Option.when(phone.trim.isEmpty)("Phone number is required"),
      Option.when(password.trim.isEmpty)("Password must be provided"),
      Option.when(password.length < 8)("Password must be at least 8 characters")
    ).flatten

    Either.cond(
      errors.isEmpty,
      User(
        id = UUID.randomUUID(),
        name = name,
        email = email,
        phone = phone,
        hashPassword = password,
        role = role,
        status = UserStatus.Active,
        createdAt = Instant.now(),
        updatedAt = None
      ),
      errors
    )
  }

  def hashPasswordValue(plainPassword: String): String =
    BCrypt.hashpw(plainPassword, BCrypt.gensalt())

  def checkPassword(plainPassword: String, hashPassword: String): Boolean =
    BCrypt.checkpw(plainPassword, hashPassword)


}


