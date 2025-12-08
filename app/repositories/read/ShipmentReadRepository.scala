package repositories.read

import domain.models.Shipment
import scala.concurrent.Future
import java.util.UUID

trait ShipmentReadRepository {
  def getById(id: UUID): Future[Option[Shipment]]
  def listAll(): Future[Seq[Shipment]]
}
