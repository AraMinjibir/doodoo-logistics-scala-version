package domain.services

import com.google.inject.{Inject, Singleton}
import domain.models.{DomainEvent, ShipmentAssigned, ShipmentCreated, ShipmentDelivered, ShipmentStatusChanged, UserAccountUpdated, UserCreated}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventBus @Inject()(
                          notificationService: NotificationService
                        )(implicit ec: ExecutionContext) {

  def publish(event: DomainEvent): Unit = {
    handle(event) // fire-and-forget
  }

  private def handle(event: DomainEvent): Future[Unit] = event match {

    case e: ShipmentCreated =>
      notificationService.sendShipmentCreated(e)

    case e: ShipmentStatusChanged =>
      notificationService.sendStatusUpdate(e)

    case e: UserCreated =>
      notificationService.sendUserCreated(e)

    case e: UserAccountUpdated =>
      notificationService.sendUserUpdate(e)

    case e: ShipmentAssigned =>
      notificationService.sendShipmentAssignment(e)

    case e: ShipmentDelivered =>
      notificationService.sendShipmentDelivered(e)
  }
}
