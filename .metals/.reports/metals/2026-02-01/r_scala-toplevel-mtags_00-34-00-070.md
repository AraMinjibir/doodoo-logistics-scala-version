error id: file://<WORKSPACE>/app/domain/models/Shipment.scala:[1063..1064) in Input.VirtualFile("file://<WORKSPACE>/app/domain/models/Shipment.scala", "package domain.models
import domain.models.ShipmentStatus.Created

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import scala.collection.mutable.ListBuffer


 final case class Dimensions private(lengthInCentimeters: Double, widthInCentimeters: Double, heightInCentimeters: Double)
object Dimensions {
def create(lengthInCentimeters: Double, widthInCentimeters: Double, heightInCentimeters: Double): Either[List[Throwable], Dimensions] = {
  val errors = ListBuffer.empty[Throwable]
  if(lengthInCentimeters <= 0)
   errors += new IllegalArgumentException(s"Package length must not be < 0: $lengthInCentimeters")

   if (widthInCentimeters <= 0)
     errors += new IllegalArgumentException(s"Width length must not be < 0: $widthInCentimeters")

   if (heightInCentimeters <= 0)
     errors += new IllegalArgumentException(s"Height length must not be < 0: $heightInCentimeters")

  if (errors.nonEmpty)
    Left(errors.toList)
  else Right(Dimensions(lengthInCentimeters, widthInCentimeters, heightInCentimeters))
}
  
  def 

}

final case class PackageDetails private(weightInKilograms: Double, dimensions: Dimensions, contents: String)
object PackageDetails {
  def createPackageDetails(weightInKilograms: Double, dimensions: Dimensions, contents: String): Either[List[Throwable], PackageDetails] = {
    val errors = ListBuffer.empty[Throwable]
    if (weightInKilograms <= 0)
      errors += new IllegalArgumentException(
        s"Weight must be greater than 0: $weightInKilograms"
      )

    if (contents.trim.isEmpty)
      errors += new IllegalArgumentException(
        "Content must not be empty"
      )


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

    if (name.trim.isEmpty)
     errors += new IllegalArgumentException(s"name must not be empty: $name")


     if (contact.trim.isEmpty)
       errors += new IllegalArgumentException(s"contact can not be empty: $contact")

    if (errors.nonEmpty)
      Left(errors.toList)
    else Right(Recipient(name, contact, address))
  }
}
final case class Address private(street: String, city: String, state: String, country: String, postalCode: String)
object Address {
  def createAddress(street: String, city: String, state: String, country: String, postalCode: String):Either[List[Throwable],Address]={
    val errors = ListBuffer.empty[Throwable]
    if (street.trim.isEmpty)
     errors += new IllegalArgumentException(s"Street name must not be empty: $street")

    if (city.trim.isEmpty)
      errors += new IllegalArgumentException(s"City name must not be empty: $city")

    if (state.trim.isEmpty)
     errors += new IllegalArgumentException(s"State must not be empty: $state")

    if (country.trim.isEmpty)
      errors += new IllegalArgumentException(s"Country name must not be empty: $country")

    if (postalCode.trim.isEmpty)
      errors += new IllegalArgumentException(s"Postal Code must not be empty: $postalCode")

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



")
file://<WORKSPACE>/file:<WORKSPACE>/app/domain/models/Shipment.scala
file://<WORKSPACE>/app/domain/models/Shipment.scala:30: error: expected identifier; obtained rbrace


Current stack trace:
java.base/java.lang.Thread.getStackTrace(Thread.java:1619)
scala.meta.internal.mtags.ScalaToplevelMtags.failMessage(ScalaToplevelMtags.scala:1250)
scala.meta.internal.mtags.ScalaToplevelMtags.$anonfun$reportError$1(ScalaToplevelMtags.scala:1236)
scala.meta.internal.metals.StdReporter.$anonfun$create$1(ReportContext.scala:148)
scala.util.Try$.apply(Try.scala:217)
scala.meta.internal.metals.StdReporter.create(ReportContext.scala:143)
scala.meta.pc.reports.Reporter.create(Reporter.java:10)
scala.meta.internal.mtags.ScalaToplevelMtags.reportError(ScalaToplevelMtags.scala:1233)
scala.meta.internal.mtags.ScalaToplevelMtags.methodIdentifier(ScalaToplevelMtags.scala:1144)
scala.meta.internal.mtags.ScalaToplevelMtags.emitTerm(ScalaToplevelMtags.scala:908)
scala.meta.internal.mtags.ScalaToplevelMtags.$anonfun$loop$16(ScalaToplevelMtags.scala:344)
scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
scala.meta.internal.mtags.MtagsIndexer.withOwner(MtagsIndexer.scala:53)
scala.meta.internal.mtags.MtagsIndexer.withOwner$(MtagsIndexer.scala:50)
scala.meta.internal.mtags.ScalaToplevelMtags.withOwner(ScalaToplevelMtags.scala:49)
scala.meta.internal.mtags.ScalaToplevelMtags.loop(ScalaToplevelMtags.scala:344)
scala.meta.internal.mtags.ScalaToplevelMtags.indexRoot(ScalaToplevelMtags.scala:96)
scala.meta.internal.metals.SemanticdbDefinition$.foreachWithReturnMtags(SemanticdbDefinition.scala:83)
scala.meta.internal.metals.Indexer.indexSourceFile(Indexer.scala:560)
scala.meta.internal.metals.Indexer.$anonfun$reindexWorkspaceSources$3(Indexer.scala:691)
scala.meta.internal.metals.Indexer.$anonfun$reindexWorkspaceSources$3$adapted(Indexer.scala:688)
scala.collection.IterableOnceOps.foreach(IterableOnce.scala:630)
scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:628)
scala.collection.AbstractIterator.foreach(Iterator.scala:1313)
scala.meta.internal.metals.Indexer.reindexWorkspaceSources(Indexer.scala:688)
scala.meta.internal.metals.MetalsLspService.$anonfun$onChange$2(MetalsLspService.scala:936)
scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
scala.concurrent.Future$.$anonfun$apply$1(Future.scala:691)
scala.concurrent.impl.Promise$Transformation.run(Promise.scala:500)
java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
java.base/java.lang.Thread.run(Thread.java:840)

}
^
#### Short summary: 

expected identifier; obtained rbrace