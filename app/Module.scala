import com.google.inject.AbstractModule
import domain.gateways.{MockPaymentGateway, PaymentGateway}
import domain.services.{PaymentService, ShipmentService, SupportCenterService, UserService}
import domain.services.impl.{PaymentServiceImpl, ShipmentServiceImpl, SupportCenterServiceImpl, UserServiceImpl}
import mappers.ShipmentRowMapper
import repositories.{PaymentRepository, ShipmentRepository, SlickPaymentRepository, SlickShipmentRepository, SlickSupportCenterRepository, SlickUserRepository, SupportCenterRepository, UserRepository}

class Module extends AbstractModule {
  override def configure(): Unit = {

    // Bind Repositories
    bind(classOf[ShipmentRepository]).to(classOf[SlickShipmentRepository])
    bind(classOf[SupportCenterRepository]).to(classOf[SlickSupportCenterRepository])
    bind(classOf[PaymentRepository]).to(classOf[SlickPaymentRepository])
    bind(classOf[UserRepository]).to(classOf[SlickUserRepository])

    // Bind Services
    bind(classOf[ShipmentService]).to(classOf[ShipmentServiceImpl])
    bind(classOf[SupportCenterService]).to(classOf[SupportCenterServiceImpl])
    bind(classOf[PaymentService]).to(classOf[PaymentServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])



    //Bind Mapper
    bind(classOf[ShipmentRowMapper]).asEagerSingleton()

    bind(classOf[PaymentGateway]).to(classOf[MockPaymentGateway])


  }
}