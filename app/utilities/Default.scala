package utilities

import com.google.inject.{Inject, Singleton}
import domain.models.{PackageDetails, Recipient}

import java.time.Instant
import java.time.temporal.ChronoUnit

@Singleton
class Default @Inject() extends CostCalculator with DateEstimator {
  override def calculate(packageDetails: PackageDetails, recipient: Recipient): BigDecimal = {
    val weight = packageDetails.weight
    val basePrice = BigDecimal(10.0)
    val ratePerKg = BigDecimal(2.5)

    basePrice + ((weight) * ratePerKg)
  }

  override def estimate(recipient: Recipient): Option[Instant] = {
    Some(Instant.now().plus(3, ChronoUnit.DAYS))
  }

}
