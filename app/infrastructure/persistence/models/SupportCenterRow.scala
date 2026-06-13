package infrastructure.persistence.models

import domain.models.{Comment, ComplaintStatus}

import java.time.Instant
import java.util.UUID

case class SupportCenterRow(
                             id: UUID,
                             userId: UUID,
                             shipmentId: UUID,
                             subject: String,
                             description: String,
                             status: ComplaintStatus,
                             createdAt:  Instant,
                             resolvedAt: Option[Instant],
                             resolvedBy: Option[UUID],
                             comment: Seq[Comment]
                           )
