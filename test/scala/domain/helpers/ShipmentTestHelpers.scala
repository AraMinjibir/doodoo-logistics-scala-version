package scala.domain.helpers

import domain.models.{Address, Dimensions, PackageDetails, Recipient, Shipment, ShipmentStatus}
import org.scalatest.Assertions._
import play.api.Application
import play.api.libs.json.{JsObject, JsValue, Json}
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
  val fixedNow = Instant.parse("2026-02-09T18:36:27Z")


  def createTestShipment(
                          id: UUID = shipmentId,
                          tracking: String = trackingNumber,
                          status: ShipmentStatus = ShipmentStatus.Created
                        ): Shipment = {

    val address = Address
      .createAddress("123 Main St", "Mjb", "Kano", "Nigeria", "100001")
      .fold(err => fail(err.map(_.getMessage).mkString(",")), identity)

    val recipient = Recipient
      .createRecipient("Test Recipient", "08012345678", address)
      .fold(err => fail(err.map(_.getMessage).mkString(",")), identity)

    val dimensions = Dimensions
      .createDimension(30.0, 20.0, 10.0)
      .fold(err => fail(err.map(_.getMessage).mkString(",")), identity)

    val packageDetails = PackageDetails
      .createPackageDetails(30.0, dimensions, "Clothing")
      .fold(err => fail(err.map(_.getMessage).mkString(",")), identity)

    Shipment(
      id = id,
      trackingNumber = Some(tracking),
      senderName = "Ara Minjibir",
      recipient = recipient,
      packageDetails = packageDetails,
      status = status,
      createdAt = testNow,
      updatedAt = testNow
    )
  }


  // DTO Template for Service Inputs

  def validShipment(sender: String = "Ara Minjibir", now: Instant = fixedNow): Shipment = {

    val address =
      Address
        .createAddress(
          "123 Main St",
          "Mjb",
          "Kano",
          "Nigeria",
          "100001"
        )
        .fold(err => fail(err.map(_.getMessage).mkString(",")), identity)

    val recipient =
      Recipient
        .createRecipient(
          name = "Test Recipient",
          contact = "08012345678",
          address = address
        )
        .fold(err => fail(err.map(_.getMessage).mkString(",")), identity)

    val dimensions =
      Dimensions
        .createDimension(30.0, 20.0, 10.0)
        .fold(err => fail(err.map(_.getMessage).mkString(",")), identity)

    val packageDetails =
      PackageDetails
        .createPackageDetails(
          weightInKilograms = 30.0,
          dimensions = dimensions,
          contents = "Clothing"
        )
        .fold(err => fail(err.map(_.getMessage).mkString(",")), identity)

    Shipment(
      id = shipmentId,
      senderName = sender,
      recipient = recipient,
      packageDetails = packageDetails,
      trackingNumber = Some(trackingNumber),
      status = ShipmentStatus.Created,
      createdAt = now,
      updatedAt = now
    )
  }
  // Default valid template
  def validCreatePayload(): JsValue =
    Json.obj(
      "senderName" -> "Ara Minjibir",

      // Recipient fields (flat)
      "name" -> "Test Recipient",
      "contact" -> "08012345678",
      "streetName" -> "Main St",
      "streetNumber" -> "123",
      "city" -> "Mjb",
      "state" -> "Kano",
      "country" -> "Nigeria",
      "postalCode" -> "100001",

      // Package fields (flat)
      "weight" -> 30.0,
      "length" -> 30.0,
      "width" -> 20.0,
      "height" -> 10.0,
      "contents" -> "Clothing"
    )


  // Template for testing validation failures
  def invalidCreatePayload: JsObject = Json.obj(
    "senderName" -> "",
    "recipient" -> Json.obj("name" -> "Missing Address Info")
  )


  def seedShipment(app: Application, sender: String = "Seed User"): Unit = {
    val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, "/shipments")
      .withJsonBody(validCreatePayload())

    // result is a Future[Result], we await it to ensure DB is seeded before test continues
    val result = route(app, request).get
    Await.result(result, 5.seconds)
  }



}