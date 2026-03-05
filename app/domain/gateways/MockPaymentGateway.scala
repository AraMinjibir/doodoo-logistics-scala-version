package domain.gateways

import com.google.inject.Singleton
import domain.models.Payment

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MockPaymentGateway()(implicit ec: ExecutionContext) extends PaymentGateway {

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

    // Skip signature verification for mock

    Right(
      PaymentWebhookEvent(
        reference = payload,
        status = "success",
        gatewayTransactionId = Some("MOCK_TX_123")
      )
    )
  }
}
