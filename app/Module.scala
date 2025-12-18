import com.google.inject.AbstractModule
import domain.services.ShipmentService
import domain.services.impl.ShipmentServiceImpl
import domain.validation.ShipmentValidation
import domain.validation.impl.ShipmentValidationImpl
import mappers.ShipmentRowMapper
import repositories.read.{ShipmentReadRepository, SlickShipmentReadRepository}
import repositories.write.{ShipmentWriteRepository, SlickShipmentWriteRepository}
import utilities.{CostCalculator, DateEstimator, Default}

class Module extends AbstractModule {
  override def configure(): Unit = {
    // Bind Repositories
    bind(classOf[ShipmentReadRepository]).to(classOf[SlickShipmentReadRepository])
    bind(classOf[ShipmentWriteRepository]).to(classOf[SlickShipmentWriteRepository])

    // Bind Services
    bind(classOf[ShipmentService]).to(classOf[ShipmentServiceImpl])
//Bind Mapper

    bind(classOf[ShipmentRowMapper]).asEagerSingleton()

    // Bind Validation
    bind(classOf[ShipmentValidation]).to(classOf[ShipmentValidationImpl])

    // Bind Utilities
    bind(classOf[CostCalculator]).to(classOf[Default])
    bind(classOf[DateEstimator]).to(classOf[Default])
  }
}