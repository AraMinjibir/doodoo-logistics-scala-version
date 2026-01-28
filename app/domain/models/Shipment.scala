package domain.models
import domain.models.ShipmentStatus.Created

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.collection.mutable.ListBuffer


 final case class Dimensions private(lengthInCentimeters: Double, widthInCentimeters: Double, heightInCentimeters: Double)
object Dimensions {
def create(lengthInCentimeters: Double, widthInCentimeters: Double, heightInCentimeters: Double): Either[List[Throwable], Dimensions] = {
  val errors = ListBuffer.empty[Throwable]
  if(lengthInCentimeters <= 0){
   errors += new IllegalArgumentException(s"Package length must not be < 0: $lengthInCentimeters")
  }
   if (widthInCentimeters <= 0) {
     errors += new IllegalArgumentException(s"Width length must not be < 0: $widthInCentimeters")
   }
   if (heightInCentimeters <= 0) {
     errors += new IllegalArgumentException(s"Height length must not be < 0: $heightInCentimeters")
   }
  if (errors.nonEmpty)
    Left(errors.toList)
  else Right(Dimensions(lengthInCentimeters, widthInCentimeters, heightInCentimeters))
}
}

final case class PackageDetails private(weightInKilograms: Double, dimensions: Dimensions, contents: String)
object PackageDetails {
  def createPackageDetails(weightInKilograms: Double, dimensions: Dimensions, contents: String): Either[List[Throwable], PackageDetails] = {
    val errors = ListBuffer.empty[Throwable]
    if (weightInKilograms <= 0) {
      errors += new IllegalArgumentException(
        s"Weight must be greater than 0: $weightInKilograms"
      )
    }
    if (contents.trim.isEmpty) {
      errors += new IllegalArgumentException(
        "Content must not be empty"
      )
    }

    if (errors.nonEmpty)
      Left(errors.toList)
    else
      Right(PackageDetails(weightInKilograms, dimensions, contents))
  }

}

final case class Recipient private(name: String, contact: String, address: Address)
object Recipient{
  def createRecipient(name: String, contact: String, address: Address):Either[List[Throwable], Recipient] = {
    val errors = ListBuffer.empty[Throwable]

    if (name.trim.isEmpty) {
     errors += new IllegalArgumentException(s"name must not be empty: $name")
    }

     if (contact.trim.isEmpty){
       errors += new IllegalArgumentException(s"contact can not be empty: $contact")
     }
    if (errors.nonEmpty)
      Left(errors.toList)
    else Right(Recipient(name, contact, address))
  }
}
final case class Address private(street: String, city: String, state: String, country: String, postalCode: String)
object Address {
  def createAddress(street: String, city: String, state: String, country: String, postalCode: String):Either[List[Throwable],Address]={
    val errors = ListBuffer.empty[Throwable]
    if (street.trim.isEmpty) {
     errors += new IllegalArgumentException(s"Street name must not be empty: $street")
    }
    if (city.trim.isEmpty) {
      errors += new IllegalArgumentException(s"City name must not be empty: $city")
    }
    if (state.trim.isEmpty){
     errors += new IllegalArgumentException(s"State must not be empty: $state")
    }
    if (country.trim.isEmpty) {
      errors += new IllegalArgumentException(s"Country name must not be empty: $street")
    }
    if (postalCode.trim.isEmpty) {
      errors += new IllegalArgumentException(s"Postal Code must not be empty: $postalCode")
    }
    if (errors.nonEmpty)
      Left(errors.toList)
    else Right(Address(street, city, state, country, postalCode))
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
                           updatedAt: Instant = Instant.now()
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


}

object Shipment {
  def generateTrackingNumber(prefix: String = "DODO"): String = {
    val id = UUID.randomUUID().toString.replace("-", "").take(12).toUpperCase
    s"$prefix-$id"
  }
}



