package infrastructure.persistence.models

import domain.models.{Address, ShipmentStatus, ProofOfDelivery}

import java.time.Instant
import java.util.UUID

final case class ShipmentRow(
                              id: UUID,
                              trackingNumber: Option[String],
                              senderName: String,
                              recipientName: String,
                              recipientStreet: String,
                              recipientCity: String,
                              recipientState: String,
                              recipientCountry: String,
                              recipientPostalCode: String,
                              recipientContact: String,
                              weight: Double,
                              length: Double,
                              width: Double,
                              height: Double,
                              contents: String,
                              status: ShipmentStatus,
                              estimatedDeliveryDate: Option[Instant],
                              createdAt: Instant,
                              updatedAt: Instant,
                              cost: BigDecimal,
                              proofOfDelivery: Seq[ProofOfDelivery]
                            ) {
  def recipientAddress: Address = Address.createAddress(
    recipientStreet,
    recipientCity,
    recipientState,
    recipientCountry,
    recipientPostalCode
  ).getOrElse(throw new Exception("Invalid address from DB"))
}
