package domain.services

import com.google.inject.{Inject, Singleton}
import domain.models.{ShipmentAssigned, ShipmentCreated, ShipmentDelivered, ShipmentStatusChanged, UserAccountUpdated, UserCreated}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationService @Inject()(
                                   emailService:EmailService
                                   ) (implicit ec: ExecutionContext) {

  def sendShipmentCreated(event: ShipmentCreated): Future[Unit] = {
    emailService.send(
      to = event.senderName,
      subject = "Shipment Created",
      body =
        s"""
           |Your shipment has been created successfully.
           |
           |Tracking Number: ${event.trackingNumber}
           |Shipment ID: ${event.shipmentId}
           |
           |You can use this tracking number to monitor delivery progress.
   """.stripMargin
    )
  }

  def sendStatusUpdate(event: ShipmentStatusChanged): Future[Unit] = {
    val message = s"Shipment ${event.shipmentId} is now ${event.newStatus}"

    for {
      _ <- emailService.send(event.senderEmail, "Shipment Update", message)
      _ <- emailService.send(event.recipientEmail, "Shipment Update", message)
    } yield ()
  }

  def sendUserCreated(event: UserCreated): Future [Unit] = {
    emailService.send(
      to = event.username,
      subject = "Account created successfully",
      body =
        s"""
           |Hello ${event.username},
           |
           |Your account has been created successfully.
           |
           |Role: ${event.role}
           |
           |You can now log in to the platform.
       """.stripMargin
    )
  }

  def sendUserUpdate(event: UserAccountUpdated): Future[Unit] = {
    emailService.send(
      to = event.email,
      subject = "Account status Updated",
      body =
        s"""
           |Your account status has been updated.
           |
           |New Status: ${event.status}
           |
           |If this change was unexpected, please contact support.
       """.stripMargin
    )
  }
  def sendShipmentAssignment(event: ShipmentAssigned): Future[Unit] = {
    emailService.send(
      to = event.username,
      subject = "Service provision Assignment",
      body = s"Service provision is Assigned to you with id: ${event.shipmentId}"
    )
  }

  def sendShipmentDelivered(event: ShipmentDelivered): Future[Unit] = {
    emailService.send(
      to = event.senderEmail,
      subject = "Shipment Delivered",
      body =
        s"""
           |Your shipment has been successfully delivered.
           |
           |Tracking Number: ${event.trackingNumber}
           |Shipment ID: ${event.shipmentId}
           |
           |Thank you for using our service.
       """.stripMargin
    )
  }
}
