import com.google.inject.AbstractModule
import domain.services.{ShipmentService, UserService}
import domain.services.impl.{ShipmentServiceImpl, UserServiceImpl}
import domain.validation.{ShipmentValidation, UserValidation}
import domain.validation.impl.{ShipmentValidationImpl, UserValidationImpl}
import mappers.ShipmentRowMapper
import repositories.read.{ShipmentReadRepository, SlickShipmentReadRepository, UserReadRepository, UserSlickReadRepository}
import repositories.write.{ShipmentWriteRepository, SlickShipmentWriteRepository, SlickUserWriteRepository, UserWriteRepository}
import utilities.{CostCalculator, DateEstimator, Default}

class Module extends AbstractModule {
  override def configure(): Unit = {

    // Bind Repositories
    bind(classOf[ShipmentReadRepository]).to(classOf[SlickShipmentReadRepository])
    bind(classOf[ShipmentWriteRepository]).to(classOf[SlickShipmentWriteRepository])
    bind(classOf[UserReadRepository]).to(classOf[UserSlickReadRepository])
    bind(classOf[UserWriteRepository]).to(classOf[SlickUserWriteRepository])

    // Bind Services
    bind(classOf[ShipmentService]).to(classOf[ShipmentServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])

  //Bind Mapper

    bind(classOf[ShipmentRowMapper]).asEagerSingleton()

    // Bind Validation
    bind(classOf[ShipmentValidation]).to(classOf[ShipmentValidationImpl])
    bind(classOf[UserValidation]).to(classOf[UserValidationImpl])

    // Bind Utilities
    bind(classOf[CostCalculator]).to(classOf[Default])
    bind(classOf[DateEstimator]).to(classOf[Default])
  }
}