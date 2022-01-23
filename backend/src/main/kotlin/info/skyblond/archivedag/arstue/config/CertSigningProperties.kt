package info.skyblond.archivedag.arstue.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(value = "cert-signing")
data class CertSigningProperties(
    val subjectDnC: String? = null,
    val subjectDnO: String? = null,
    val subjectDnOU: String? = null,
)
