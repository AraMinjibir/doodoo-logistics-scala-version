import com.google.inject.AbstractModule
import domain.services.{ShipmentService, SupportCenterService}
import domain.services.impl.{ShipmentServiceImpl, SupportCenterServiceImpl}
import mappers.ShipmentRowMapper
import repositories.{ShipmentRepository, SlickShipmentRepository, SlickSupportCenterRepository, SupportCenterRepository}

class Module extends AbstractModule {
  override def configure(): Unit = {

    // Bind Repositories
    bind(classOf[ShipmentRepository]).to(classOf[SlickShipmentRepository])
    bind(classOf[SupportCenterRepository]).to(classOf[SlickSupportCenterRepository])



    // Bind Services
    bind(classOf[ShipmentService]).to(classOf[ShipmentServiceImpl])
    bind(classOf[SupportCenterService]).to(classOf[SupportCenterServiceImpl])


    //Bind Mapper

    bind(classOf[ShipmentRowMapper]).asEagerSingleton()

  }
}