package domain.services

import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import io.jsonwebtoken.security.Keys
import javax.inject.{Inject, Singleton}
import play.api.Configuration

import java.time.Instant
import java.util.Date
import domain.models.User

@Singleton
class JwtService @Inject()(config: Configuration) {

  private val secretKey: String = config.get[String]("jwt.secretKey")
  private val issuer: String = config.get[String]("jwt.issuer")
  private val expiryMinutes: Long = config.get[Long]("jwt.expirationMinutes")

  private val signingKey = Keys.hmacShaKeyFor(secretKey.getBytes("UTF-8"))

  def generateToken(user: User): String = {

    val now = Instant.now()
    val expiry = now.plusSeconds(expiryMinutes * 60)

    Jwts.builder()
      .setIssuer(issuer)
      .setSubject(user.id.toString)
      .claim("email", user.email)
      .claim("name", user.name)
      .claim("role", user.role.toString)
      .setIssuedAt(Date.from(now))
      .setExpiration(Date.from(expiry))
      .signWith(signingKey, SignatureAlgorithm.HS256)
      .compact()
  }
}