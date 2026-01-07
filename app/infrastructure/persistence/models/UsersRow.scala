package infrastructure.persistence.models

import domain.models.{User, UsersRole}

import java.util.UUID

case class UsersRow(
                     id:UUID,
                     name: String,
                     email: String,
                     hashPassword: String,
                     phoneNumber: String,
                     role:UsersRole
                   )