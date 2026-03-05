import com.google.inject.AbstractModule
import domain.gateways.{PaymentGateway, PaystackGateway}
import domain.services.{PaymentService, ShipmentService, SupportCenterService}
import domain.services.impl.{PaymentServiceImpl, ShipmentServiceImpl, SupportCenterServiceImpl}
import mappers.ShipmentRowMapper
import repositories.{PaymentRepository, ShipmentRepository, SlickPaymentRepository, SlickShipmentRepository, SlickSupportCenterRepository, SupportCenterRepository}

class Module extends AbstractModule {
  override def configure(): Unit = {

    // Bind Repositories
    bind(classOf[ShipmentRepository]).to(classOf[SlickShipmentRepository])
    bind(classOf[SupportCenterRepository]).to(classOf[SlickSupportCenterRepository])
    bind(classOf[PaymentRepository]).to(classOf[SlickPaymentRepository])

    // Bind Services
    bind(classOf[ShipmentService]).to(classOf[ShipmentServiceImpl])
    bind(classOf[SupportCenterService]).to(classOf[SupportCenterServiceImpl])
    bind(classOf[PaymentService]).to(classOf[PaymentServiceImpl])


    //Bind Mapper
    bind(classOf[ShipmentRowMapper]).asEagerSingleton()

    bind(classOf[PaymentGateway]).to(classOf[PaystackGateway])


  }
}