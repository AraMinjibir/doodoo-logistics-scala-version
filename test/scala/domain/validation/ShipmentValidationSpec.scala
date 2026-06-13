package scala.domain.validation

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ShipmentValidationSpec extends AnyWordSpec with Matchers{

  sealed trait ShipmentStatus
  object ShipmentStatus {
    case object Created extends ShipmentStatus
    case object InTransit extends ShipmentStatus
    case object OutForDelivery extends ShipmentStatus
    case object Delivered extends ShipmentStatus
    case object Cancelled extends ShipmentStatus

    // A helper function for error messages
    def toString(status: ShipmentStatus): String = status.toString
  }

  class ShipmentValidation {
    import ShipmentStatus._

    private val allowedTransitions: Map[ShipmentStatus, Set[ShipmentStatus]] = Map(
      // Current -> Allowed Next Statuses
      Created -> Set(InTransit, Cancelled),
      InTransit -> Set(OutForDelivery, Cancelled),
      OutForDelivery -> Set(Delivered, Cancelled),
      Delivered -> Set.empty,
      Cancelled -> Set.empty
    )

    def validateTransition(current: ShipmentStatus, next: ShipmentStatus): Either[String, Unit] = {
      val allowedNext = allowedTransitions.getOrElse(current, Set.empty)

      if (!allowedNext.contains(next))
        Left(s"Invalid shipment status transition from ${ShipmentStatus.toString(current)} to ${ShipmentStatus.toString(next)}")
      else
        Right(())
    }
  }


  val validation = new ShipmentValidation()
  import ShipmentStatus._

  "ShipmentValidation.validateTransition" should {

    "return Right(()) for a legal transition (e.g., Created to InTransit)" in {
      val current = Created
      val next = InTransit

      val result = validation.validateTransition(current, next)

      // Assertion for Success
      result shouldBe Right(())
    }

    "return Right(()) for a legal transition to Cancelled (e.g., InTransit to Cancelled)" in {
      val current = InTransit
      val next = Cancelled

      val result = validation.validateTransition(current, next)

      // Assertion for Success
      result shouldBe Right(())
    }

    "return Left(error) for an illegal transition (e.g., Delivered to Created)" in {
      val current = Delivered
      val next = Created

      val result = validation.validateTransition(current, next)

      // Assertion for Failure: Check that it is a Left
      result.isLeft shouldBe true

    }

    "return Left(error) for skipping a step (e.g., Created to Delivered)" in {
      val current = Created
      val next = Delivered

      val result = validation.validateTransition(current, next)

      // Assertion for Failure
      result.isLeft shouldBe true
    }

    "return Left(error) when transitioning from a terminal state (e.g., Delivered to InTransit)" in {
      val current = Delivered
      val next = InTransit

      val result = validation.validateTransition(current, next)

      // Assertion for Failure
      result.isLeft shouldBe true
    }
  }

}
