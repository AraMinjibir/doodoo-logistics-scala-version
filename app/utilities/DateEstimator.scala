package utilities

import domain.models.Recipient

import java.time.Instant

trait DateEstimator {
  def estimate(recipient: Recipient): Option[Instant]
}
