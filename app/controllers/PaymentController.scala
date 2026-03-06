package controllers

import com.google.inject.{Inject, Singleton}
import controllers.dto.{PaymentRequestDto, PaymentResponseDto}
import controllers.helpers.ResultMapper
import domain.models.{PaymentMethod, PaymentStatus}
import domain.services.PaymentService
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, RawBuffer}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentController @Inject()(
                                 paymentService: PaymentService,
                                 cc: ControllerComponents
                                 )(implicit ex:ExecutionContext)
                                  extends AbstractController(cc)
                                    with ResultMapper{

  def makePayment(callbackUrl: String):Action[JsValue] = Action.async(parse.json){ implicit request =>
    request.body.validate[PaymentRequestDto].fold(
     errors =>  Future.successful(onValidationError(errors)),

      paymentDto => paymentDto.toPaymentDomain match {
        case Left(err) => Future.successful(BadRequest(Json.toJson(err)))
        case Right(validPayment) =>
          paymentService.initiatePayment(validPayment, callbackUrl).map { paymentUrl =>
            Created(Json.obj("authorizationUrl" -> paymentUrl))
          }.recover {
            case e => handleException(e)
          }
      }
    )

  }
  def handleWebhook: Action[RawBuffer] = Action.async(parse.raw) { request =>

    val payload = request.body.asBytes().map(_.utf8String).getOrElse("")
    val signature = request.headers.get("x-paystack-signature").getOrElse("")

    paymentService
      .handleWebhook(payload, signature)
      .map {
        case Left(error) =>
          Unauthorized(error)

        case Right(payment) =>
          Ok(s"Payment updated: ${payment.referenceNumber}")
      }
      .recover{
        case ex =>
          handleException(ex)
      }
  }

  def getPaymentById(ref: String):Action[AnyContent] = Action.async{
    paymentService.getPaymentById(ref).map{
      case Some(payment) => Ok(Json.toJson(PaymentResponseDto.toPaymentResponseDto(payment)))
      case None => NotFound(Json.obj("message" -> s"No payment found with the given id:$ref"))
    }.recover{
      case ex =>
        handleException(ex)
    }
  }
  def getPaymentStatus(status:PaymentStatus):Action[AnyContent] = Action.async{
    paymentService.getPaymentStatus(status).map{ paymentByStatus =>
      val paymentDto = paymentByStatus.map(PaymentResponseDto.toPaymentResponseDto)
      Ok(Json.toJson(paymentDto))

    }.recover{
      case ex =>
        handleException(ex)
    }
  }
  def getPaymentByMethod(method: PaymentMethod):Action[AnyContent] = Action.async{
    paymentService.getPaymentByMethod(method).map{ paymentByMethod =>
      val paymentDto = paymentByMethod.map(PaymentResponseDto.toPaymentResponseDto)

      Ok(Json.toJson(paymentDto))
    }.recover{
      case e => handleException(e)
    }
  }

  def getAllPayments: Action[AnyContent] = Action.async{
    paymentService.getAllPayment.map{ payments =>
      val paymentDto = payments.map(PaymentResponseDto.toPaymentResponseDto)
      Ok(Json.toJson(paymentDto))
    }.recover{
      case e => handleException(e)
    }
  }
  def deletePayment(ref:String):Action[AnyContent] = Action.async{
    paymentService.deletePayment(ref).map{
      case Right(_) => NoContent
      case Left(err) => BadRequest(Json.obj("message" -> err))
    }
  }

  def getDailyRevenue(date: String): Action[AnyContent] = Action.async {

    val parsedDate = LocalDate.parse(date)

    paymentService.getDailyRevenue(parsedDate).map { revenue =>
      Ok(Json.obj(
        "date" -> parsedDate,
        "dailyRevenue" -> revenue
      ))
    }.recover {
      case e => handleException(e)
    }

  }
  def getWeeklyRevenue(date: String): Action[AnyContent] = Action.async {

    val parsedDate = LocalDate.parse(date)

    paymentService.getWeeklyRevenue(parsedDate).map { revenue =>
      Ok(Json.obj(
        "weekReferenceDate" -> parsedDate,
        "weeklyRevenue" -> revenue
      ))
    }.recover {
      case e => handleException(e)
    }

  }
  def getMonthlyRevenue(year: Int, month: Int): Action[AnyContent] = Action.async {

    paymentService.getMonthlyRevenue(year, month).map { revenue =>
      Ok(Json.obj(
        "year" -> year,
        "month" -> month,
        "monthlyRevenue" -> revenue
      ))
    }.recover {
      case e => handleException(e)
    }

  }


}
