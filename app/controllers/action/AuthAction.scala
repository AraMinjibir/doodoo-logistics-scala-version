package controllers.action

import domain.models.UserRole.{Sender, ServiceProvider}

import javax.inject.Inject
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import domain.models.{User, UserRole}
import domain.services.JwtService
import repositories.UserRepository

// WrappedRequest containing the authenticated User
case class AuthenticatedRequest[A](user: User, request: Request[A]) extends WrappedRequest[A](request)

class AuthAction @Inject()(
                            parser: BodyParsers.Default,
                            jwtService: JwtService,
                            authRepository: UserRepository
                          )(implicit ec: ExecutionContext) extends ActionBuilder[AuthenticatedRequest, AnyContent] {

  override def parser: BodyParser[AnyContent] = parser
  override protected def executionContext: ExecutionContext = ec

  // Allowed authenticated users
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    authenticate(request, block, allowedRoles = Set(UserRole.Admin, UserRole.CustomerSupportAgent, UserRole.Recipient, Sender, ServiceProvider))

  // Helper to restrict to certain roles
  def withRole(allowedRoles: Set[UserRole]): ActionBuilder[AuthenticatedRequest, AnyContent] =
    new ActionBuilder[AuthenticatedRequest, AnyContent] {
      override def parser: BodyParser[AnyContent] = parser
      override protected def executionContext: ExecutionContext = ec

      override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
        authenticate(request, block, allowedRoles)
    }

  private def authenticate[A](
                               request: Request[A],
                               block: AuthenticatedRequest[A] => Future[Result],
                               allowedRoles: Set[UserRole]
                             ): Future[Result] = {
    request.headers.get("Authorization") match {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.stripPrefix("Bearer ").trim
        jwtService.validateToken(token) match {
          case Some(claims) =>
            authRepository.findUserByEmail(claims.email).flatMap {
              case Some(user) if allowedRoles.contains(user.role) =>
                block(AuthenticatedRequest(user, request))
              case Some(_) =>
                Future.successful(Results.Forbidden("Not authorized for this action"))
              case None =>
                Future.successful(Results.Unauthorized("User not found"))
            }
          case None =>
            Future.successful(Results.Unauthorized("Invalid token"))
        }
      case _ =>
        Future.successful(Results.Unauthorized("Missing token"))
    }
  }
}
