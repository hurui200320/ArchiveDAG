package info.skyblond.archivedag.arudaz.config

import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.util.concurrent.TimeUnit

@ConstructorBinding
@ConfigurationProperties(value = "jwt")
data class JwtProperties(
    val secret: String? = null,
    val expireInDuration: Long = 5,
    val expireInUnit: TimeUnit = TimeUnit.MINUTES,
    val signatureAlgorithm: SignatureAlgorithm = SignatureAlgorithm.HS512
)
