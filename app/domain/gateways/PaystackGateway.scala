package domain.gateways

import com.google.inject.{Inject, Singleton}
import domain.models.Payment
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaystackGateway @Inject()(ws: WSClient,
                                config: Configuration

                               )(implicit ec: ExecutionContext)  extends  PaymentGateway {

  private val secret = config.get[String]("paystack.secret")

  override def initiatePayment(
                                payment: Payment,
                                callbackUrl: String
                              ): Future[PaymentGatewayResponse] = {

    val payload = Json.obj(
      "amount" -> (payment.amount * 100).toLong,
      "reference" -> payment.referenceNumber,
      "callback_url" -> callbackUrl
    )

    ws.url("https://api.paystack.co/transaction/initialize")
      .addHttpHeaders(
        "Authorization" -> s"Bearer $secret"
      )
      .post(payload)
      .map { response =>
        val url = (response.json \ "data" \ "authorization_url").as[String]
        val ref = (response.json \ "data" \ "reference").as[String]

        PaymentGatewayResponse(url, ref)
      }
  }

  override def verifyWebhook(
                              payload: String,
                              signature: String
                            ): Either[String, PaymentWebhookEvent] = {

    // HMAC verification logic here

    Right(
      PaymentWebhookEvent(
        reference = "...",
        status = "...",
        gatewayTransactionId = None
      )
    )
  }


}
