package scala.domain.helpers

import domain.models.{Address, Dimensions, PackageDetails, Recipient, Shipment, ShipmentStatus}
import play.api.Application
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait ShipmentTestHelpers {

//  Test fixtures
val fixedInstant: Instant = Instant.parse("2026-01-10T10:00:00Z")
  val shipmentId: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val trackingNumber: String = "TRACK-123"

  def testNow: Instant = Instant.now()

  def createTestShipment(
                          id: UUID = shipmentId,
                          tracking: String = trackingNumber,
                          status: ShipmentStatus = ShipmentStatus.Created
                        ): Shipment = Shipment(
    id = id,
    trackingNumber = Some(tracking),
    senderName = "Ara Minjibir",
    recipient = Recipient(
      name = "Test Recipient",
      address = Address("123 Main St", "Mjb", "Kano", "Nigeria", "100001"),
      contact = "08012345678"
    ),
    packageDetails = PackageDetails(30.0, Dimensions(30.0, 20.0, 10.0), "Clothing"),
    status = status,
    estimatedDeliveryDate = None,
    createdAt = testNow,
    updatedAt = testNow,
    cost = BigDecimal(5000),
    history = Seq.empty
  )

  // DTO Template for Service Inputs

  def validCreateDto(sender: String = "Ara Minjibir"): Shipment =
    Shipment(
      id = shipmentId,
      senderName = sender,
      recipient = Recipient(
        name = "Test Recipient",
        address = Address("123 Main St", "Mjb", "Kano", "Nigeria", "100001"),
        contact = "08012345678"
      ),
      trackingNumber = Some(trackingNumber),
      packageDetails = PackageDetails(
        weight = 30.0,
        dimensions = Dimensions(30.0, 20.0, 10.0),
        contents = "Clothing"
      ),
      status = ShipmentStatus.Created,
      estimatedDeliveryDate = None,
      createdAt = Instant.now(),
      updatedAt = Instant.now(),
      cost = BigDecimal(5000),
      history = Seq.empty
    )

  // Default valid template
  def validCreatePayload(sender: String = "Ara Minjibir"): JsObject = Json.obj(
    "senderName" -> sender,
    "recipient" -> Json.obj(
      "name" -> "Test Recipient",
      "contact" -> "08012345678",
      "address" -> Json.obj(
        "street" -> "123 Main St",
        "city" -> "Kano",
        "state" -> "Kano",
        "country" -> "Nigeria",
        "postalCode" -> "100001"
      )
    ),
    "packageDetails" -> Json.obj(
      "weight" -> 10.5,
      "dimensions" -> Json.obj("length" -> 20, "width" -> 15, "height" -> 10),
      "contents" -> "Books"
    )
  )

  // Template for testing validation failures
  def invalidCreatePayload: JsObject = Json.obj(
    "senderName" -> "",
    "recipient" -> Json.obj("name" -> "Missing Address Info")
  )


  def seedShipment(app: Application, sender: String = "Seed User"): Unit = {
    val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, "/shipments")
      .withJsonBody(validCreatePayload(sender))

    // result is a Future[Result], we await it to ensure DB is seeded before test continues
    val result = route(app, request).get
    Await.result(result, 5.seconds)
  }



}