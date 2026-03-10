package domain.models

import java.time.Instant
import java.util.UUID


  case class User(
                   id: UUID,
                   name: String,
                   email: String,
                   phone: String,
                   role: UserRole,
                   status: UserStatus,
                   createdAt: Instant,
                   updatedAt: Instant
                 )


