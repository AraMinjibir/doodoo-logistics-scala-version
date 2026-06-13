package controllers.json


import domain.models.{PaymentMethod, PaymentStatus}
import domain.models.PaymentStatus.fromString
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

object PaymentStatusJson {
  implicit val format: Format[PaymentStatus] = new Format[PaymentStatus] {

    override def writes(status: PaymentStatus): JsValue =
      JsString(PaymentStatus.toString(status))

    override def reads(json: JsValue): JsResult[PaymentStatus] =
      json match {
        case JsString(value) =>
         PaymentStatus.fromString(value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Invalid payment status: $value"))

        case _ => JsError("PaymentStatus must be a string")
      }
  }
}

object PaymentMethodJson {
  implicit val format: Format[PaymentMethod] = new Format[PaymentMethod] {

    override def writes(method: PaymentMethod): JsValue =
      JsString(PaymentMethod.toString(method))

    override def reads(json: JsValue): JsResult[PaymentMethod] =
      json match {
        case JsString(value) =>
         PaymentMethod.fromString(value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Invalid payment method: $value"))

        case _ => JsError("PaymentMethod must be a string")
      }
  }
}
