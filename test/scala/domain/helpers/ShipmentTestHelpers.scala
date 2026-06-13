package scala.domain.helpers

import domain.errors.DomainError
import domain.models.{Address, Dimensions, PackageDetails, ProofOfDelivery, Recipient, Shipment, ShipmentStatus, User, UserRole, UserStatus}
import org.scalatest.Assertions._
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.Application
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSClient
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
  val providerId = UUID.randomUUID()

  def createTestShipment(
                          id: UUID = shipmentId,
                          tracking: String = trackingNumber,
                          status: ShipmentStatus = ShipmentStatus.Created,
                          serviceProviderId: Option[UUID] = None
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
      updatedAt = testNow,
      serviceProviderId = serviceProviderId
    )
  }

def uploadProofOfDelivery: ProofOfDelivery = {
  val image = Some("https://fiqiufqijqooqjiq");
  val note = "Proof of delivery uploaded";
  val submittedBy = "DooDoo Logistics";
  val submittedAt = fixedNow

  ProofOfDelivery(
    image = image,
    note = note,
    submittedBy = submittedBy,
    submittedAt = submittedAt
  )
}

  def validProofOfDelivery: Either[List[DomainError], ProofOfDelivery] = {
    ProofOfDelivery.createProofOfDelivery(
      image = Some("https://doodooImage.png"),
      note = "DooDoo proof of delivery after delivered",
      submittedBy = "DooDoo Logistics User",
      submittedAt = fixedNow
    )
  }
  def validProof: ProofOfDelivery = validProofOfDelivery match {
    case Right(proof) => proof
    case Left(errors) => fail(s"Expected valid proof but got errors: $errors")
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
      updatedAt = now,
      serviceProviderId = None
    )
  }

  val serviceProvider: User = User(
    id = providerId,
    name = "Service Provider",
    email = "sp@test.com",
    hashPassword = "serviceProvider12",
    phone = "123456789",
    role = UserRole.ServiceProvider,
    status = UserStatus.Active,
    createdAt = Instant.now(),
    updatedAt = None
  )
  // Default valid template
  val validCreatePayload: JsValue = Json.obj(
    "senderName"   -> "Ara Minjibir",        // required by DTO
    "streetName"   -> "Main St",
    "streetNumber" -> "123",
    "city"         -> "Mjb",
    "state"        -> "Kano",
    "country"      -> "Nigeria",
    "postalCode"   -> "100001",
    "contact"      -> "08012345678",
    "weight"       -> 30.0,
    "length"       -> 30.0,
    "width"        -> 20.0,
    "height"       -> 10.0,
    "contents"     -> "Clothing"
  )

  def proofPayload = Json.obj(
    "image" -> "https://doodooImage.png",
    "note" -> "Delivered successfully",
    "submittedBy" -> "Ara",
    "submittedAt" -> "2025-01-01T10:00:00Z"
  )


  // Template for testing validation failures
  def invalidCreatePayload: JsObject = Json.obj(
    "senderName" -> "",
    "recipient" -> Json.obj("name" -> "Missing Address Info")
  )


  def seedShipment(app: Application, sender: String = "Seed User"): Unit = {
    val request: FakeRequest[AnyContentAsJson] = FakeRequest(POST, "/shipments")
      .withJsonBody(validCreatePayload)

    // result is a Future[Result], we await it to ensure DB is seeded before test continues
    val result = route(app, request).get
    Await.result(result, 5.seconds)
  }

  def loginAndGetUser(wsClient: WSClient, email: String, password: String, port: Int): (String, String) = {
    val response = Await.result(
      wsClient
        .url(s"http://localhost:$port/users/login")
        .post(Json.obj(
          "email" -> email,
          "hashPassword" -> password
        )),
      5.seconds
    )

    println("LOGIN RESPONSE: " + response.body)

    response.status mustBe OK

    val token = (response.json \ "token").as[String]
    val id = (response.json \ "id").as[String]

    (token, id)
  }

  val senderPayload: JsObject = Json.obj(
    "name" -> "Sender User",
    "email" -> "sender@mail.com",
    "password" -> "password123",
    "phone" -> "07000000001",
    "role" -> "Sender"
  )

  val adminPayload: JsObject = Json.obj(
    "name" -> "Admin User",
    "email" -> "admin@mail.com",
    "password" -> "password123",
    "phone" -> "07000000002",
    "role" -> "Admin"
  )
  val agentPayload: JsObject = Json.obj(
    "name" -> "Support Agent",
    "email" -> "supportagent@mail.com",
    "password" -> "password123",
    "phone" -> "07000000002",
    "role" -> "CustomerSupportAgent"
  )

  val providerPayload: JsObject = Json.obj(
    "name" -> "Service Provider",
    "email" -> "provider@mail.com",
    "password" -> "password123",
    "phone" -> "07000000003",
    "role" -> "ServiceProvider"
  )

  val recipientPayload: JsObject = Json.obj(
    "name" -> "Recipient User",
    "email" -> "recipient@mail.com",
    "password" -> "password123",
    "phone" -> "07000000004",
    "role" -> "Recipient"
  )



}