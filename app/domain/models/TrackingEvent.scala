package domain.models

case class TrackingEvent(
                          status: ShipmentStatus,
                          timestamp: java.time.Instant,
                          location: Option[String]
                        )


