package controllers

import com.google.inject.{Inject, Singleton}
import controllers.action.AuthAction
import dto.{CreateShipmentDto, ProofOfDeliveryDto, ShipmentResponseDto}
import controllers.helpers.ResultMapper
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc._
import domain.models.{ShipmentStatus, UserRole}
import controllers.json.ShipmentStatusJson._
import domain.errors.ShipmentNotFoundById
import domain.models.UserRole.{Admin, CustomerSupportAgent, Recipient, ServiceProvider}
import domain.services.ShipmentService
import play.api.Logger
import play.mvc.Security.AuthenticatedAction

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ShipmentController @Inject()(shipmentService: ShipmentService,
                                   authenticatedAction: AuthAction,
                                   cc:ControllerComponents) (implicit ec:ExecutionContext)
  extends AbstractController(cc) with ResultMapper {
  private val logger = Logger(classOf[ShipmentController])

  def createShipment: Action[JsValue] =
    authenticatedAction
      .withRole(Set(UserRole.Sender))
      .async(parse.json) { implicit request =>

        request.body.validate[CreateShipmentDto].fold(

          errors =>
            Future.successful(onValidationError(errors)),

          shipmentDto =>
            shipmentService
              .createShipment(
                shipmentDto.toShipment.copy(senderName = request.user.id.toString)
              )
              .map {
                case Right(shipment) =>
                  Created(Json.toJson(CreateShipmentDto.toDto(shipment)))

                case Left(error) =>
                  toResult(error)
              }
              .recover { case e => handleException(e) }
        )
      }
  def getShipmentByTrackingNumber(trackingNumber: String): Action[AnyContent] =
    authenticatedAction
      .withRole(Set(UserRole.Sender, UserRole.Recipient))
      .async {
        shipmentService.getShipmentByTrackingNumber(trackingNumber).map {
          case Some(shipment) =>
            Ok(Json.toJson(CreateShipmentDto.toDto(shipment)))

          case None =>
            NotFound(Json.obj("message" -> s"Shipment $trackingNumber not found"))
        }
      }

  def getShipmentById(shipmentId: UUID): Action[AnyContent] =
    authenticatedAction.withRole(Set(Admin, CustomerSupportAgent,ServiceProvider))
      .async {
    shipmentService.getShipmentById(shipmentId).map {
      // 1. Found: Map domain model to DTO and return 200 OK
      case Some(shipment) => Ok(Json.toJson(CreateShipmentDto.toDto(shipment)))
      // 2. Not Found: Return 404 with a clear message
      case None => NotFound(Json.obj(s"message" -> s"Shipment with this id: $shipmentId is not found"))
    }.recover {
      case e => handleException(e)
    }
  }

  def getShipmentByStatus(shipmentStatus: ShipmentStatus): Action[AnyContent] =
    authenticatedAction
      .withRole(Set(UserRole.Admin, CustomerSupportAgent))
      .async {
        shipmentService.getShipmentByStatus(shipmentStatus).map { shipments =>
          val dtos = shipments.map(CreateShipmentDto.toDto)
          Ok(Json.toJson(dtos))
        }.recover {
          case e => handleException(e)
        }
      }

  def getAllShipments(page: Int, pageSize: Int): Action[AnyContent] =
    authenticatedAction
      .withRole(Set(UserRole.Admin))
      .async {
        val offset = (page - 1) * pageSize

        shipmentService.listShipments(offset, pageSize).map { shipments =>
          val shipmentDtos = shipments.map(CreateShipmentDto.toDto)

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

  def updateShipment(id: UUID): Action[JsValue] =
    authenticatedAction
      .withRole(Set(UserRole.Admin))
      .async(parse.json) { implicit request =>

        request.body.validate[CreateShipmentDto].fold(

          errors =>
            Future.successful(
              BadRequest(Json.obj("errors" -> JsError.toJson(errors)))
            ),

          shipmentData => {

            val updatedDomain = CreateShipmentDto.toDomain(shipmentData)

            shipmentService.updateShipment(id, updatedDomain).map {

              case Left(err) if err.message.contains("not found") =>
                NotFound(Json.obj("message" -> err.message))

              case Left(msg) =>
                BadRequest(Json.obj("message" -> msg.message))

              case Right(updated) =>
                Ok(Json.toJson(CreateShipmentDto.toDto(updated)))
            }
          }
        ).recover {
          case e => handleException(e)
        }
      }

  def updateShipmentStatus(trackingNumber: String): Action[JsValue] =
    authenticatedAction
      .withRole(Set(UserRole.ServiceProvider))
      .async(parse.json) { implicit request =>

        val statusResult = (request.body \ "status").validate[ShipmentStatus]
        val location = (request.body \ "location").asOpt[String]

        statusResult.fold(

          errors =>
            Future.successful(
              BadRequest(Json.obj("message" -> "Invalid status provided"))
            ),

          newStatus =>

            shipmentService
              .updateShipmentStatus(trackingNumber, newStatus, location)
              .map {

                case Left(error) if error.message.contains("not found") =>
                  NotFound(Json.obj("error" -> error.message))

                case Left(error) =>
                  BadRequest(Json.obj("error" -> error.message))

                case Right(updatedShipment) =>
                  Ok(Json.toJson(CreateShipmentDto.toDto(updatedShipment)))
              }
              .recover {
                case e => handleException(e)
              }
        )
      }

  def deleteShipment(id: UUID): Action[AnyContent] =
    authenticatedAction.withRole(Set(Recipient))
      .async {
        shipmentService.deleteShipment(id).map {
      case Right(_) => NoContent
      case Left(err) => BadRequest(Json.obj("error" -> err.message))
    }.recover {
      case e => handleException(e)
    }
  }

  def uploadProofOfDelivery(trackingNumber: String): Action[JsValue] =
    authenticatedAction
      .withRole(Set(UserRole.Recipient))
      .async(parse.json) { implicit request =>

        request.body.validate[ProofOfDeliveryDto].fold(
          errors => Future.successful(onValidationError(errors)),
          proofDto =>
          ProofOfDeliveryDto.toProofOfDeliveryDomain(proofDto) match {

            case Left(validationErrors) =>
              Future.successful(
                BadRequest(
                  Json.obj("errors" -> validationErrors.map(_.message))
                )
              )

            case Right(validProof) =>
              shipmentService
                .uploadProofOfDelivery(trackingNumber, validProof)
                .map {

                  case Left(serviceErrors) =>
                    BadRequest(
                      Json.obj("errors" -> serviceErrors.map(_.message))
                    )

                  case Right(updatedShipment) =>

                    Ok(
                      Json.toJson(
                        ShipmentResponseDto.fromDomain(updatedShipment)

                      )
                    )
                }
          }
      )
    }

  def assignServiceProvider(shipmentId: UUID, providerId: UUID): Action[AnyContent] =
    authenticatedAction.withRole(Set(Admin)).async {
    shipmentService.assignServiceProviderToShipment(shipmentId, providerId).map {
      case Right(updatedShipment) => Ok(Json.toJson(CreateShipmentDto.toDto(updatedShipment)))
      case Left(err)              => toResult(err)
    }
  }

  def assignedShipments: Action[AnyContent] =
    authenticatedAction
      .withRole(Set(UserRole.ServiceProvider))
      .async { implicit request =>

        val providerId = request.user.id

        shipmentService
          .getShipmentsForProvider(providerId)
          .map { shipments =>
            Ok(Json.toJson(shipments.map(ShipmentResponseDto.fromDomain)))
          }
      }
}



