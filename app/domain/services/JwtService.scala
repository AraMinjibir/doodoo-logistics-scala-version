package domain.services

import io.jsonwebtoken.{JwtException, Jwts, SignatureAlgorithm}
import io.jsonwebtoken.security.Keys

import javax.inject.{Inject, Singleton}
import play.api.Configuration

import java.time.Instant
import java.util.Date
import domain.models.{User, UserRole}

case class JwtClaims(
                      sub: String,
                      email: String,
                      name: String,
                      role: UserRole,
                      iat: Instant,
                      exp: Instant
                    )

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

  // Validate token and extract claims
  def validateToken(token: String): Option[JwtClaims] = {
    try {
      val parsed = Jwts.parserBuilder()
        .setSigningKey(signingKey)
        .build()
        .parseClaimsJws(token)
        .getBody

      UserRole.fromString(parsed.get("role", classOf[String])).map { role =>
        JwtClaims(
          sub = parsed.getSubject,
          email = parsed.get("email", classOf[String]),
          name = parsed.get("name", classOf[String]),
          role = role,
          iat = Instant.ofEpochMilli(parsed.getIssuedAt.getTime),
          exp = Instant.ofEpochMilli(parsed.getExpiration.getTime)
        )
      }
    } catch {
      case _: Exception => None
    }
  }
}