package domain.models
import domain.errors.{DomainError, DuplicateProofOfDelivery, InvalidShipmentStatusTransition, ProofMustContainImageOrNote, ShipmentNotDelivered, SubmittedByEmpty}
import domain.models.ShipmentStatus.{Created, Delivered}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID


 final case class Dimensions private(lengthInCentimeters: Double, widthInCentimeters: Double, heightInCentimeters: Double)
object Dimensions {
  def createDimension(lengthInCentimeters: Double, widthInCentimeters: Double, heightInCentimeters: Double): Either[List[Throwable], Dimensions] = {
    val err: List[IllegalArgumentException] = List(
      Option.when(lengthInCentimeters <= 0)(s"Package length must not be < 0: $lengthInCentimeters"),
      Option.when(widthInCentimeters <= 0)(s"Width length must not be < 0: $widthInCentimeters"),
      Option.when(heightInCentimeters <= 0)(s"Height length must not be < 0: $heightInCentimeters"),
    )
    .flatten
    .map(new IllegalArgumentException(_))

    Either.cond(
      err.isEmpty,
      Dimensions(lengthInCentimeters, widthInCentimeters, heightInCentimeters),
      err
    )
}
}

final case class PackageDetails private(weightInKilograms: Double, dimensions: Dimensions, contents: String)
object PackageDetails {
  def createPackageDetails(weightInKilograms: Double, dimensions: Dimensions, contents: String): Either[List[Throwable], PackageDetails] = {
    val errors : List[IllegalArgumentException] = List(
      Option.when(weightInKilograms <= 0 )( s"Weight must be greater than 0: $weightInKilograms"),
      Option.when(contents.trim.isEmpty)(s"Content must not be empty: $contents"),
    )
      .flatten
      .map(new IllegalArgumentException(_))
      Either.cond(
        errors.isEmpty,
          PackageDetails(weightInKilograms,dimensions,contents),
        errors
      )
  }

}

final case class Recipient private(name: String, contact: String, address: Address)
object Recipient{
  def createRecipient(name: String, contact: String, address: Address):Either[List[Throwable], Recipient] = {
    val errors: List[IllegalArgumentException] = List(
      Option.when(name.trim.isEmpty)(s"name must not be empty: $name"),
      Option.when(contact.trim.isEmpty)(s"contact can not be empty: $contact")
    )
      .flatten
      .map(new IllegalArgumentException(_))

    Either.cond(
      errors.isEmpty,
      Recipient(name,contact,address),
      errors
    )
  }
}
final case class Address private(street: String, city: String, state: String, country: String, postalCode: String)
object Address {
  def createAddress(street: String, city: String, state: String, country: String, postalCode: String):Either[List[Throwable],Address]={
    val errors : List[IllegalArgumentException] = List(
      Option.when(street.trim.isEmpty)(s"Street name must not be empty: $street"),
      Option.when(city.trim.isEmpty)(s"City name must not be empty: $city"),
      Option.when(state.trim.isEmpty)(s"State must not be empty: $state"),
      Option.when(country.trim.isEmpty)(s"Country name must not be empty: $country"),
      Option.when(postalCode.trim.isEmpty)(s"Postal Code must not be empty: $postalCode")
    )
      .flatten
      .map(new IllegalArgumentException(_))
    Either.cond(
      errors.isEmpty,
      Address(street,city,state,country,postalCode),
      errors
    )
  }
}
final case class ProofOfDelivery private(
                            image:Option[String],
                            note:String,
                            submittedBy: String,
                            submittedAt:Instant
                          )
object ProofOfDelivery {
  def createProofOfDelivery(
                             image: Option[String],
                             note: String,
                             submittedBy: String,
                             submittedAt: Instant
                           ): Either[List[DomainError], ProofOfDelivery] = {

    val cleanedImage     = image.map(_.trim).filter(_.nonEmpty)
    val cleanedNote      = note.trim
    val cleanedSubmitter = submittedBy.trim

    val errors: List[DomainError] = List(
      Option.when(cleanedImage.isEmpty && cleanedNote.isEmpty)(
        ProofMustContainImageOrNote
      ),
      Option.when(cleanedSubmitter.isEmpty)(
        SubmittedByEmpty
      )
    ).flatten

    Either.cond(
      errors.isEmpty,
      ProofOfDelivery(cleanedImage, cleanedNote, cleanedSubmitter, submittedAt),
      errors
    )
  }


}

final case class Shipment(
                           senderName: String,
                           recipient: Recipient,
                           packageDetails: PackageDetails,
                           id: UUID =  UUID.randomUUID(),
                           trackingNumber: Option[String]  = Some(Shipment.generateTrackingNumber()),
                           status: ShipmentStatus = Created,
                           createdAt: Instant = Instant.now(),
                           updatedAt: Instant = Instant.now(),
                           proofOfDelivery: Seq[ProofOfDelivery] = Seq.empty,
                         ) {
  def cost: BigDecimal = {
    val weight = packageDetails.weightInKilograms
    val basePrice = BigDecimal(10.0)
    val ratePerKg = BigDecimal(2.5)

    basePrice + ((weight) * ratePerKg)
  }

   def deliveryDateEstimate: Option[Instant] = {
    Some(Instant.now().plus(3, ChronoUnit.DAYS))
  }
  def updateStatus(
                    next: ShipmentStatus,
                    now: Instant = Instant.now()
                  ): Either[DomainError, Shipment] =
    ShipmentStatus
      .validateTransition(status, next)
      .map { _ =>
        copy(
          status = next,
          updatedAt = now
        )
      }


  def updatedShipment(senderName: String,
                      recipient: Recipient,
                      packageDetails: PackageDetails,
                      now:Instant = Instant.now()):Shipment = copy(
    senderName = senderName,
    recipient = recipient,
    packageDetails = packageDetails,
    updatedAt = now
  )

  def attachProofOfDelivery(
                             proof: ProofOfDelivery
                           ): Either[DomainError, Shipment] = {

    if (status != Delivered)
      Left(ShipmentNotDelivered)

    else if (proofOfDelivery.exists(_.submittedBy == proof.submittedBy))
      Left(DuplicateProofOfDelivery)

    else
      Right(
        copy(
          proofOfDelivery = proofOfDelivery :+ proof,
          updatedAt = Instant.now()
        )
      )
  }


}

object Shipment {
  def generateTrackingNumber(prefix: String = "DODO"): String = {
    val id = UUID.randomUUID().toString.replace("-", "").take(12).toUpperCase
    s"$prefix-$id"
  }
}



