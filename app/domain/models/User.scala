package domain.models

import java.util.UUID

case class User(
                id:UUID,
                name: String,
                email: String,
                hashPassword: String,
                phoneNumber: String,
                role:UsersRole
                )
