package domain.gateways

import com.google.inject.{Inject, Singleton}
import domain.models.Payment
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MockPaymentGateway @Inject() (implicit ec: ExecutionContext) extends PaymentGateway {

  override def initiatePayment(
                                payment: Payment,
                                callbackUrl: String
                              ): Future[PaymentGatewayResponse] = {

    val fakeUrl =
      s"https://mock-payments.local/pay/${payment.referenceNumber}"

    Future.successful(
      PaymentGatewayResponse(
        authorizationUrl = fakeUrl,
        reference = payment.referenceNumber
      )
    )
  }

  override def verifyWebhook(
                              payload: String,
                              signature: String
                            ): Either[String, PaymentWebhookEvent] = {
    // Parse the JSON payload
    val json = try {
      Right(Json.parse(payload))
    } catch {
      case _: Throwable => Left("Invalid JSON payload")
    }

    json.flatMap { j =>
      (j \ "data" \ "reference").asOpt[String] match {
        case Some(ref) =>
          Right(
            PaymentWebhookEvent(
              reference = ref,
              status = "success",
              gatewayTransactionId = Some("MOCK_TX_123")
            )
          )
        case None => Left("Reference not found in payload")
      }
    }
  }
}
