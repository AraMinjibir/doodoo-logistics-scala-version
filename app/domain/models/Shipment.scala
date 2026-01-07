package domain.models
import java.time.Instant
import java.util.UUID

// Case classes representing a  rows in DB table.
 final case class Dimensions(length: BigDecimal, width: BigDecimal, height: BigDecimal)

final case class PackageDetails(weight: BigDecimal, dimensions: Dimensions, contents: String)

final case class Recipient(name: String, contact: String, address: Address)
final case class Address(street: String, city: String, state: String, country: String, postalCode: String)



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
                           history: Seq[TrackingEvent] = Seq.empty,
                           updatedAt: Instant
                         )

