package domain.models

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
              phone: String,
              role: UserRole
            ): Either[List[String], User] = {
     val err: List[String] = List(
      Option.when(name.trim.isEmpty)(s"Name must be provided: $name"),
      Option.when(email.trim.isEmpty)(s"Email address must be provided: $email"),
      Option.when(phone.trim.isEmpty)(s"Phone number is required"),
       Option.when(!UserRole.values.contains(role))(
         s"Invalid user role: $role"
       )
    ).flatten
    Either.cond(
      err.isEmpty,
      User(id = UUID.randomUUID(),
        name = name,
        email = email,
        phone = phone,
        hashPassword = "",
        role = role,
        status = UserStatus.Active,
        createdAt = Instant.now(),
        updatedAt = None
      ), err
    )
  }
}


