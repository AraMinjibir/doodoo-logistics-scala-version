import com.google.inject.AbstractModule
import domain.services.ShipmentService
import domain.services.impl.ShipmentServiceImpl
import mappers.ShipmentRowMapper
import repositories.{ShipmentRepository, SlickShipmentRepository}

class Module extends AbstractModule {
  override def configure(): Unit = {

    // Bind Repositories
    bind(classOf[ShipmentRepository]).to(classOf[SlickShipmentRepository])

    // Bind Services
    bind(classOf[ShipmentService]).to(classOf[ShipmentServiceImpl])

  //Bind Mapper

    bind(classOf[ShipmentRowMapper]).asEagerSingleton()

  }
}