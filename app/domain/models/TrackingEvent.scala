package domain.models

import play.api.libs.json.{Json, OFormat}

case class TrackingEvent(
                          status: ShipmentStatus,
                          timestamp: java.time.Instant,
                          location: Option[String]
                        )

object TrackingEvent {
  implicit val format: OFormat[TrackingEvent] = Json.format[TrackingEvent]
}
