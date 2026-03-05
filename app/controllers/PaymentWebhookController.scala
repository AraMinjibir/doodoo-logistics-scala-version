package controllers

import com.google.inject.{Inject, Singleton}
import domain.gateways.PaymentGateway
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

@Singleton
class PaymentWebhookController @Inject()(gateway: PaymentGateway,
                                         cc: ControllerComponents
                                        )(implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def handleWebhook = Action(parse.raw) { request =>

    val payload = request.body.asBytes().get.utf8String
    val signature = request.headers.get("x-paystack-signature").getOrElse("")

    gateway.verifyWebhook(payload, signature) match {

      case Left(error) =>
        Unauthorized(error)

      case Right(event) =>
        Ok(s"Webhook received for ${event.reference}")
    }
  }

}
