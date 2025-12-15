package controllers

import com.google.inject.Inject
import domain.services.impl.ShipmentServiceImpl
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import api.dto.CreateShipmentDto
import mappers.ShipmentMapper
import scala.concurrent.{ExecutionContext, Future}

class ShipmentController @Inject()(shipmentService: ShipmentServiceImpl,
                                   cc:ControllerComponents) (implicit ec:ExecutionContext)
  extends AbstractController(cc){

  def createShipment: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CreateShipmentDto].fold(
      // 1. Handle JSON parsing errors (400 Bad Request)
      errors => Future.successful(BadRequest(
        Json.obj("message" -> "Invalid request format", "details" -> errors.toString())
      )),

      shipmentData => {
        shipmentService.createShipment(shipmentData)
          .map { createdShipment =>
            // 2. Success: Use 201 Created and return the new resource DTO
            Created(Json.toJson(ShipmentMapper.toDto(createdShipment)))
          }
          .recoverWith {
            // 3. Handle specific domain exceptions from the service
            case e: IllegalArgumentException =>
              Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
            case e: Exception =>
              // Fallback for unexpected errors (500 Internal Server Error)
              Future.successful(InternalServerError(Json.obj("error" -> "An unexpected error occurred")))
          }
      }
    )
  }
}
