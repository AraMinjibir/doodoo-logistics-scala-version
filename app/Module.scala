import com.google.inject.AbstractModule
import domain.services.{ShipmentService, UserService}
import domain.services.impl.{ShipmentServiceImpl, UserServiceImpl}
import domain.validation.{ShipmentValidation, UserValidation}
import domain.validation.impl.{ShipmentValidationImpl, UserValidationImpl}
import mappers.{ShipmentRowMapper, UserRowMapper}
import repositories.{ShipmentRepository, SlickShipmentRepository, SlickUserRepository, UserRepository}
import utilities.{CostCalculator, DateEstimator, Default}

class Module extends AbstractModule {
  override def configure(): Unit = {

    // Bind Repositories
    bind(classOf[ShipmentRepository]).to(classOf[SlickShipmentRepository])
    bind(classOf[UserRepository]).to(classOf[SlickUserRepository])

    // Bind Services
    bind(classOf[ShipmentService]).to(classOf[ShipmentServiceImpl])
    bind(classOf[UserService]).to(classOf[UserServiceImpl])

  //Bind Mapper

    bind(classOf[ShipmentRowMapper]).asEagerSingleton()
    bind(classOf[UserRowMapper]).asEagerSingleton()

    // Bind Validation
    bind(classOf[ShipmentValidation]).to(classOf[ShipmentValidationImpl])
    bind(classOf[UserValidation]).to(classOf[UserValidationImpl])

    // Bind Utilities
    bind(classOf[CostCalculator]).to(classOf[Default])
    bind(classOf[DateEstimator]).to(classOf[Default])
  }
}