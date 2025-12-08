package domain.models

import java.time.Instant
import java.util.UUID

// Case classes representing a  rows in DB table.
final case class Dimensions(length: Double, width: Double, height: Double)

final case class PackageDetails(weight: Double, dimensions: Dimensions, contents: String)

final case class Recipient(name: String, address: String, contact: String)

final case class Shipment(
                           id: UUID,
                           trackingNumber: Option[String],
                           senderName: String,
                           recipient: Recipient,
                           packageDetails: PackageDetails,
                           status: ShipmentStatus,
                           estimatedDeliveryDate: Option[Instant],
                           createdAt: Instant,
                           cost: BigDecimal,
                           history: Seq[TrackingEvent] = Seq.empty
                         )
