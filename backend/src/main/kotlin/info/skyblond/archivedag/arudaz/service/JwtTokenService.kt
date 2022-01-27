package info.skyblond.archivedag.arudaz.service

import info.skyblond.archivedag.commons.service.EtcdConfigService
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

@Service
class JwtTokenService(
    private val configService: EtcdConfigService,
) {
    private val logger = LoggerFactory.getLogger(JwtTokenService::class.java)
    private val etcdNamespace = "arudaz/jwt"

    // Hard coded sig alg
    private val signatureAlgorithm = SignatureAlgorithm.HS512

    private val secretKeyEtcdConfigKey = "hs512_secret_key"

    private val expireInMinuteEtcdConfigKey = "expire_in_minute"
    private val defaultExpireInMinute: Long = 120

    private fun getExpireMinutes(): Long =
        configService.getLong(etcdNamespace, expireInMinuteEtcdConfigKey, defaultExpireInMinute)

    private fun getSigningKey(): SecretKey {
        val result = configService.getByteArray(etcdNamespace, secretKeyEtcdConfigKey)
        if (result == null) {
            logger.warn("No JWT secret found, generating one...")
            val generated = Keys.secretKeyFor(signatureAlgorithm)
            configService.setByteArray(etcdNamespace, secretKeyEtcdConfigKey, generated.encoded)
            return generated
        }
        return Keys.hmacShaKeyFor(result)
    }

    fun generateToken(userDetails: UserDetails): String {
        val currentTime = System.currentTimeMillis()
        val duration = TimeUnit.MINUTES.toMillis(getExpireMinutes())
        return Jwts.builder()
            .setSubject(userDetails.username)
            .setIssuedAt(Date(currentTime))
            .setNotBefore(Date(currentTime))
            .setExpiration(Date(currentTime + duration))
            .signWith(getSigningKey(), signatureAlgorithm)
            .compact()
    }

    fun getUsernameFromToken(token: String?): String? {
        val claims: Claims = try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
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
