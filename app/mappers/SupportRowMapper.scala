package mappers

import com.google.inject.Singleton
import domain.models.Complaint
import infrastructure.persistence.models.SupportCenterRow

@Singleton
class SupportRowMapper {

    def fromDomain(c: Complaint): SupportCenterRow =
      SupportCenterRow(
        id = c.id,
        userId = c.userId,
        shipmentId = c.shipmentId,
        subject = c.subject,
        description = c.description,
        status = c.status,
        createdAt = c.createdAt,
        resolvedAt = c.resolvedAt,
        resolvedBy = c.resolvedBy,
        comment = c.comment
      )

    def toDomain(row: SupportCenterRow): Complaint =
      Complaint(
        id = row.id,
        userId = row.userId,
        shipmentId = row.shipmentId,
        subject = row.subject,
        description = row.description,
        status = row.status,
        createdAt = row.createdAt,
        resolvedAt = row.resolvedAt,
        resolvedBy = row.resolvedBy,
        comment = row.comment
      )

}
