package utilities

import domain.models.{PackageDetails, Recipient}

trait CostCalculator {
  def calculate(packageDetails: PackageDetails, recipient: Recipient): BigDecimal
}
