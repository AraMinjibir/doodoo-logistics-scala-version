package controllers.dto

import controllers.dto.CreateShipmentDto.toDomain
import domain.models._
import play.api.libs.json.{Json, OFormat}

import java.time.Instant
import java.util.UUID

private[controllers] final case class DimensionsDto(
                                length: BigDecimal,
                                width: BigDecimal,
                                height: BigDecimal
                              )
private[controllers] object DimensionsDto {
  implicit val format: OFormat[DimensionsDto] = Json.format[DimensionsDto]
}

private[controllers] final case class PackageDetailsDto(
                                    weight: BigDecimal,
                                    dimensions: DimensionsDto,
                                    contents: String
                                  )
private[controllers] object PackageDetailsDto {
  implicit val format: OFormat[PackageDetailsDto] = Json.format[PackageDetailsDto]
}

private[controllers] final case class RecipientDto(
                               name: String,
                               address: Address,
                               contact: String
                             ) {
  def toRecipientDto(domainRecipient: domain.models.Recipient): RecipientDto = {
    RecipientDto(
      name = domainRecipient.name,
      address = domainRecipient.address,
      contact = domainRecipient.contact
    )
  }
}
private[controllers] object RecipientDto {
  implicit val format: OFormat[RecipientDto] = Json.format[RecipientDto]
}

private[controllers] final case class CreateShipmentDto(
                                    senderName: String,
                                    recipient: RecipientDto,
                                    packageDetails: PackageDetailsDto,

                                  ) {
 def toShipment = toDomain(this)





//  override def createShipment(shipment: Shipment): Future[Either[String,Shipment]] = {
//    validation.validateCreate(dto) match {
//      case Left(err) => Future.failed(new IllegalArgumentException(err))
//      case Right(_) =>
//        val now = Instant.now()
//        val base = ShipmentMapper.toDomain(dto)
//        val trackingNumber = TrackingNumberGenerator.generate()
//
//        val initialEvent = TrackingEvent(
//          status = ShipmentStatus.Created,
//          timestamp = now,
//          location = None
//        )
//
//        val shipmentToSave =
//          base.copy(
//            trackingNumber = Some(trackingNumber),
//            createdAt = now,
//            history = Seq(initialEvent),
//            cost = costCalculator.calculate(base.packageDetails, base.recipient),
//            estimatedDeliveryDate = dateEstimator.estimate(base.recipient)
//          )
//
//        repo.create(shipmentToSave).map{
//          case Success(_) => Right(shipmentToSave)
//          case Failure(ex) => Left(ex.getMessage)
//        }
//    }
//  }
}
private[controllers] object CreateShipmentDto {
  implicit val format: OFormat[CreateShipmentDto] = Json.format[CreateShipmentDto]

  def toDomain(dto: CreateShipmentDto): Shipment = {
    val now = Instant.now()
    val locationString: String =
      s"${dto.recipient.address.street}, ${dto.recipient.address.city}, ${dto.recipient.address.state}"

    Shipment(
      id = UUID.randomUUID(),
      trackingNumber = None,
      senderName = dto.senderName,
      recipient = Recipient(
        name = dto.recipient.name,
        address = dto.recipient.address,
        contact = dto.recipient.contact
      ),
      packageDetails = PackageDetails(
        weight = dto.packageDetails.weight,
        dimensions = Dimensions(
          length = dto.packageDetails.dimensions.length,
          width = dto.packageDetails.dimensions.width,
          height = dto.packageDetails.dimensions.height
        ),
        contents = dto.packageDetails.contents
      ),
      status = ShipmentStatus.Created,
      estimatedDeliveryDate = None,
      createdAt = now,
      updatedAt = now,
      cost = BigDecimal(0),
      history = Seq(
        TrackingEvent(
          status = ShipmentStatus.Created,
          timestamp = now,
          location = Some(locationString)
        )
      )
    )
  }

  def toDto(domain: Shipment): ShipmentResponseDto = {
    ShipmentResponseDto(
      id = domain.id,
      trackingNumber = domain.trackingNumber,
      senderName = domain.senderName,
      recipient = toRecipientDto(domain.recipient),
      packageDetails =toPackageDetailsDto(domain.packageDetails),
      status = domain.status,
      estimatedDeliveryDate = domain.estimatedDeliveryDate,
      createdAt = domain.createdAt,
      cost = domain.cost,
      history = domain.history.map(toTrackingEventDto)
    )
  }
  def toRecipientDto(domainRecipient: domain.models.Recipient): RecipientDto = {
    RecipientDto(
      name = domainRecipient.name,
      address = domainRecipient.address,
      contact = domainRecipient.contact
    )
  }
  def toDimensionsDto(domainDimensions: domain.models.Dimensions): DimensionsDto = {
    DimensionsDto(
      length = domainDimensions.length,
      width = domainDimensions.width,
      height = domainDimensions.height
    )
  }
  def toPackageDetailsDto(domainPackage: domain.models.PackageDetails): PackageDetailsDto = {
    PackageDetailsDto(
      weight = domainPackage.weight,
      dimensions = toDimensionsDto(domainPackage.dimensions),
      contents = domainPackage.contents
    )
  }
  def toTrackingEventDto(domainEvent: TrackingEvent): TrackingEventDto = {
    TrackingEventDto(
      status = domainEvent.status,
      timestamp = domainEvent.timestamp,
      location = domainEvent.location
    )
  }
}

