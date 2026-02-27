package controllers

import com.google.inject.{Inject, Singleton}
import controllers.dto.{CommentRequestDto, CommentResponseDto, ComplaintRequestDto, ComplaintResponseDto}
import controllers.helpers.ResultMapper
import domain.models.{Comment, ComplaintStatus}
import domain.services.SupportCenterService
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SupportCenterController @Inject()(
                                       supportCenterService: SupportCenterService,
                                       cc: ControllerComponents
                                       )(implicit ec:ExecutionContext) extends AbstractController(cc) with ResultMapper{

  def sendComplaint: Action[JsValue] = Action.async(parse.json){ request =>
    request.body.validate[ComplaintRequestDto].fold(
      errors => Future.successful(onValidationError(errors)),

      complaintDto => complaintDto.toComplaint match {
        case Left(err) => Future.successful(BadRequest(Json.toJson(err)))
        case Right(validComplaint) =>
        supportCenterService.createComplaint(validComplaint).map{
          case Right(saved) => Created(Json.toJson(ComplaintRequestDto.fromDomain(saved)))
          case Left(error) => toResult(error)
        }.recover{
          case e =>
            handleException(e)
        }
      })
}
  def getComplaintById(complaintId:UUID):Action[AnyContent] = Action.async {
    supportCenterService.getComplaintById(complaintId).map{
      case Some(complaint) => Ok(Json.toJson(ComplaintResponseDto.toDto(complaint)))
      case None =>  NotFound(Json.obj("message" -> s"Complaint with id: $complaintId not found"))
    }.recover{
      case e => handleException(e)
    }
  }
  def getAllComplaint:Action[AnyContent] = Action.async{
    supportCenterService.getAllComplaint.map{ complaint =>
      val complaintDto = complaint.map(ComplaintResponseDto.toDto)
      Ok(Json.toJson(complaintDto))
    }.recover{
      case ex => handleException(ex)
    }
  }
  def getComplaintByStatus(status:ComplaintStatus):Action[AnyContent] = Action.async{
    supportCenterService.getComplaintByStatus(status).map{ complaintBystatus =>
      val complaintDto = complaintBystatus.map(ComplaintResponseDto.toDto)
      Ok(Json.toJson(complaintDto))
    }.recover{
      case ex => handleException(ex)
    }
  }
  def markComplaintAsInProgress( complaintId: UUID):Action[AnyContent] = Action.async{
    supportCenterService.markComplaintAsInProgress(complaintId).map{
      case Right(updatedComplaint) => Ok(Json.toJson(ComplaintResponseDto.toDto(updatedComplaint)))
      case Left(err) => toResult(err)
    }.recover{
      case  e => handleException(e)
    }
  }
  def markComplaintAsResolved(complaintId: UUID, agentId: UUID):Action[AnyContent] = Action.async{
    supportCenterService.markComplaintAsResolved(complaintId, agentId).map{
      case Right(resolvedValue) => Ok(Json.toJson(ComplaintResponseDto.toDto(resolvedValue)))
      case Left(error) => toResult(error)
    }.recover{
      case e => handleException(e)
    }
  }
  def addComment(complaintId: UUID): Action[JsValue] =
    Action.async(parse.json) { implicit request =>

      request.body.validate[CommentRequestDto].fold(

        errors =>
          Future.successful(onValidationError(errors)),

        dto =>
          dto.toCommentDomain match {

            case Left(validationErrors) =>
              Future.successful(
                BadRequest(Json.obj("errors" -> validationErrors))
              )

            case Right(newComment) =>
              supportCenterService
                .addComment(complaintId,newComment)
                .map {
                  case Right(savedComment) =>
                    Ok(Json.toJson(CommentResponseDto.toCommentDto(savedComment)))
                  case Left(error) =>
                    toResult(error)
                }
                .recover {
                  case e => handleException(e)
                }
          }
      )
    }

}
