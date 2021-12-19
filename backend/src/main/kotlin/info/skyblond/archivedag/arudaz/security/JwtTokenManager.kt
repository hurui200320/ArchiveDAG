package info.skyblond.archivedag.arudaz.security

import info.skyblond.archivedag.arudaz.config.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenManager(private val jwtProperties: JwtProperties) {
    private val logger = LoggerFactory.getLogger(JwtTokenManager::class.java)
    private var signingKey: SecretKey? = null

    init {
        if (jwtProperties.secret == null) {
            logger.warn("No JWT secret found, generating one...")
            signingKey = Keys.secretKeyFor(jwtProperties.signatureAlgorithm)
            logger.warn(
                "Generated JWT secret key: {}",
                Base64.getEncoder().encodeToString(signingKey!!.encoded)
            )
        } else {
            signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.secret))
        }
    }

    fun generateToken(userDetails: UserDetails): String {
        val currentTime = System.currentTimeMillis()
        val duration = jwtProperties.expireInUnit.toMillis(jwtProperties.expireInDuration)
        return Jwts.builder()
            .setSubject(userDetails.username)
            .setIssuedAt(Date(currentTime))
            .setNotBefore(Date(currentTime))
            .setExpiration(Date(currentTime + duration))
            .signWith(signingKey, jwtProperties.signatureAlgorithm)
            .compact()
    }

    fun getUsernameFromToken(token: String?): String? {
        val claims: Claims = try {
            Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: JwtException) {
            logger.debug("Error when decoding JWT token", e)
            return null
        }
        return claims.subject
    }
}
