package api.dto

import play.api.libs.json.{Json, OFormat}


case class CreateShipmentDto(
                              senderName: String,
                              recipientName: String,
                              recipientAddress: String,
                              recipientContact: String,
                              weight: Double,
                              length: Double,
                              width: Double,
                              height: Double,
                              contents: String
                            )

object CreateShipmentDto {
  implicit val format: OFormat[CreateShipmentDto] = Json.format[CreateShipmentDto]
}
