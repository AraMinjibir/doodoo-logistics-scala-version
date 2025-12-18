package controllers

import com.google.inject.{Inject, Singleton}
import domain.services.impl.ShipmentServiceImpl
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._
import api.dto.CreateShipmentDto
import domain.models.ShipmentStatus
import mappers.ShipmentMapper

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShipmentController @Inject()(shipmentService: ShipmentServiceImpl,
                                   cc:ControllerComponents) (implicit ec:ExecutionContext)
  extends AbstractController(cc) {

  def createShipment: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateShipmentDto].fold(
      // 1. Handle JSON parsing errors (400 Bad Request)
      errors => Future.successful(
        BadRequest(Json.obj(
          "status" -> "Error",
          "message" -> "Validation failed",
          "errors" -> JsError.toJson(errors)
        ))
      ),

      shipmentData => {
        shipmentService.createShipment(shipmentData)
          .map { createdShipment =>
            // 2. Success: Use 201 Created and return the new resource DTO
            Created(Json.toJson(ShipmentMapper.toDto(createdShipment)))
          }
          .recover {
            // 3. Handle specific domain exceptions from the service
            case e: IllegalArgumentException => BadRequest(Json.obj("error" -> e.getMessage))
            // Fallback for unexpected errors (500 Internal Server Error)
            case _ => InternalServerError(Json.obj("error" -> "Unexpected error"))
          }
      }
    )
  }

  def getShipmentByTrackingNumber(trackingNumber: String): Action[AnyContent] = Action.async { implicit request =>
    shipmentService.getShipmentByTrackingNumber(trackingNumber).map {
      case Some(shipment) =>
        // 1. Found: Map domain model to DTO and return 200 OK
        Ok(Json.toJson(ShipmentMapper.toDto(shipment)))

      case None =>
        // 2. Not Found: Return 404 with a clear message
        NotFound(Json.obj("message" -> s"Shipment with tracking number $trackingNumber not found"))
    }.recover {
      case e: Exception =>
        // 3. Error: Log the error and return 500
        InternalServerError(Json.obj("error" -> "Internal server error occurred while retrieving shipment"))
    }
  }

  def getShipmentById(shipmentId: UUID): Action[AnyContent] = Action.async { implicit request =>
    shipmentService.getShipmentById(shipmentId).map {
      // 1. Found: Map domain model to DTO and return 200 OK
      case Some(shipment) => Ok(Json.toJson(ShipmentMapper.toDto(shipment)))
      // 2. Not Found: Return 404 with a clear message
      case None => NotFound(Json.obj(s"message" -> s"Shipment with this id: $shipmentId is not found"))
    }.recover {
      case e: Exception =>
        // 3. Error: Log the error and return 500
        InternalServerError(Json.obj("error" -> "Internal server error occurred while retrieving shipment"))
    }
  }

  def getShipmentByStatus(shipmentStatus: ShipmentStatus): Action[AnyContent] = Action.async { implicit request =>
    shipmentService.getShipmentByStatus(shipmentStatus).map { shipments =>
      val dtos = shipments.map(ShipmentMapper.toDto)
      Ok(Json.toJson(dtos))
    }.recover {
      case e: Exception => InternalServerError(Json.obj("error" -> "Failed to retrieve shipments"))
    }
  }

  def getAllShipments: Action[AnyContent] = Action.async { implicit request =>
    shipmentService.listShipments().map { shipments =>
      // 1. Map the Seq[Shipment] to Seq[ShipmentResponseDto]
      val shipmentDtos = shipments.map(ShipmentMapper.toDto)
      // 2. Wrap the DTO sequence in a Json.toJson call and return 200 OK
      Ok(Json.toJson(shipmentDtos))
    }.recover {
      case e: Exception => InternalServerError(Json.obj("error" -> "Failed to retrieve shipments"))
    }
  }

  def updateShipment(id: UUID): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateShipmentDto].fold(
      errors => Future.successful(BadRequest(Json.obj("errors" -> JsError.toJson(errors)))),

      shipmentData => {
        shipmentService.updateShipment(id, shipmentData).map {
          case Left(errorMsg) if errorMsg.contains("not found") =>
            NotFound(Json.obj("message" -> errorMsg))

          case Left(errorMsg) =>
            BadRequest(Json.obj("message" -> errorMsg))

          case Right(updatedShipment) =>
            Ok(Json.toJson(ShipmentMapper.toDto(updatedShipment)))
        }
      }
    )
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
            Ok(Json.toJson(ShipmentMapper.toDto(updatedShipment)))
        }
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
        // 204 No Content is the standard for successful deletion
        NoContent
    }
  }
}
