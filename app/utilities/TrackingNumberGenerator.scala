package utilities

import java.util.UUID

object TrackingNumberGenerator {
  // Format: DODO-<12chars>
  def generate(prefix: String = "DODO"): String = {
    val id = UUID.randomUUID().toString.replace("-", "").take(12).toUpperCase
    s"$prefix-$id"
  }
}
