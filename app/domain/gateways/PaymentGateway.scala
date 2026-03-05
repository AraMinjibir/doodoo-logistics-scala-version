package domain.gateways

import domain.models.Payment

import scala.concurrent.Future

trait PaymentGateway {

  def initiatePayment(
                       payment: Payment,
                       callbackUrl: String
                     ): Future[PaymentGatewayResponse]

  def verifyWebhook(
                     payload: String,
                     signature: String
                   ): Either[String, PaymentWebhookEvent]

}
case class PaymentGatewayResponse(
                                   authorizationUrl: String,
                                   reference: String
                                 )

case class PaymentWebhookEvent(
                                reference: String,
                                status: String,
                                gatewayTransactionId: Option[String]
                              )