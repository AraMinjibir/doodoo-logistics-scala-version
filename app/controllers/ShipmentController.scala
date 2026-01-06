package controllers

import com.google.inject.{Inject, Singleton}
import dto.CreateShipmentDto
import controllers.helpers.ResultMapper
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._
import domain.models. ShipmentStatus
import domain.services.ShipmentService
import play.api.Logger

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShipmentController @Inject()(shipmentService: ShipmentService,
                                   cc:ControllerComponents) (implicit ec:ExecutionContext)
  extends AbstractController(cc) with ResultMapper {
  private val logger = Logger(classOf[ShipmentController])

  def createShipment: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateShipmentDto].fold(
      // 1. Handle JSON parsing errors (400 Bad Request)
      errors => Future.successful(onValidationError(errors)),

      shipmentDto => {
        shipmentService.createShipment(shipmentDto.toShipment)
          .map {
            case Right(shipment) => Created(Json.toJson(CreateShipmentDto.toDto(shipment)))
            case Left(error) => BadRequest(error)
          }
          .recover {
            case e => handleException(e)
          }
      }
    )
  }

  def getShipmentByTrackingNumber(trackingNumber: String): Action[AnyContent] = Action.async { implicit request =>
    shipmentService.getShipmentByTrackingNumber(trackingNumber).map {
      case Some(shipment) =>
        // 1. Found: Map domain model to DTO and return 200 OK
        Ok(Json.toJson(CreateShipmentDto.toDto(shipment)))

      case None =>
        // 2. Not Found: Return 404 with a clear message
        NotFound(Json.obj("message" -> s"Shipment with tracking number $trackingNumber not found"))
    }.recover {
      case e => handleException(e)
    }
  }

  def getShipmentById(shipmentId: UUID): Action[AnyContent] = Action.async { implicit request =>
    shipmentService.getShipmentById(shipmentId).map {
      // 1. Found: Map domain model to DTO and return 200 OK
      case Some(shipment) => Ok(Json.toJson(CreateShipmentDto.toDto(shipment)))
      // 2. Not Found: Return 404 with a clear message
      case None => NotFound(Json.obj(s"message" -> s"Shipment with this id: $shipmentId is not found"))
    }.recover {
      case e => handleException(e)
    }
  }

  def getShipmentByStatus(shipmentStatus: ShipmentStatus): Action[AnyContent] = Action.async { implicit request =>
    shipmentService.getShipmentByStatus(shipmentStatus).map { shipments =>
      val dtos = shipments.map(CreateShipmentDto.toDto)
      Ok(Json.toJson(dtos))
    }.recover {
      case e => handleException(e)
    }
  }

  def getAllShipments(page: Int, pageSize: Int): Action[AnyContent] = Action.async { implicit request =>
    // 1. Calculate the offset for the database
    val offset = (page - 1) * pageSize

    shipmentService.listShipments(offset, pageSize).map { shipments =>
      // 2. Map domain models to DTOs
      val shipmentDtos = shipments.map(CreateShipmentDto.toDto)

      // 3. Return an "Envelope" response

      Ok(Json.obj(
        "metadata" -> Json.obj(
          "page" -> page,
          "pageSize" -> pageSize,
          "count" -> shipmentDtos.size
        ),
        "data" -> shipmentDtos
      ))
    }.recover {
      case e => handleException(e)
    }
  }
  def updateShipment(id: UUID): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateShipmentDto].fold(
      errors => Future.successful(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),

      shipmentData => {
        val updatedDomain = CreateShipmentDto.toDomain(shipmentData)
        shipmentService.updateShipment(id,updatedDomain).map{
          case Left(msg) if msg.contains("not found") =>
            NotFound(Json.obj("message" -> msg))

          case Left(msg) =>
            BadRequest(Json.obj("message" -> msg))
          case Right(updated) => Ok(Json.toJson(CreateShipmentDto.toDto(updated)))
        }
        }).recover {
      case e => handleException(e)
    }

  }

  def updateShipmentStatus(trackingNumber: String): Action[JsValue] = Action.async(parse.json) { request =>
    // 1. Extract the new status from the request body
    val statusResult = (request.body \ "status").validate[ShipmentStatus]
    val location = (request.body \ "location").asOpt[String]

    statusResult.fold(
      errors => Future.successful(BadRequest(Json.obj("message" -> "Invalid status provided"))),

      newStatus => {
        // 2. Call the Service
        shipmentService.updateShipmentStatus(trackingNumber, newStatus, location).map {
          case Left(error) if error.contains("not found") =>
            NotFound(Json.obj("error" -> error))

          case Left(error) =>
            // This captures validation failures like illegal transitions
            BadRequest(Json.obj("error" -> error))

          case Right(updatedShipment) =>
            // 3. Return the updated domain model as a DTO
            Ok(Json.toJson(CreateShipmentDto.toDto(updatedShipment)))
        }
      }.recover {
        case e => handleException(e)
      }
    )
  }
  def deleteShipment(id: UUID): Action[AnyContent] = Action.async { implicit request =>
    shipmentService.deleteShipment(id).map {
      case Left(error) if error.contains("not found") =>
        NotFound(Json.obj("message" -> error))

      case Left(error) =>
        BadRequest(Json.obj("message" -> error))

      case Right(_) =>
        // 204 No Content
        NoContent
    }.recover {
      case e => handleException(e)
    }
  }
}
