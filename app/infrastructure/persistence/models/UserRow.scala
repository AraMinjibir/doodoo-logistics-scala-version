package infrastructure.persistence.models

import domain.models.{UserRole, UserStatus}

import java.time.Instant
import java.util.UUID

case class UserRow(
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