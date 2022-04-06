package info.skyblond.archivedag.arstue.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(value = "cert-signing")
data class CertSigningProperties(
    /**
     * Subject C: Country name
     * */
    val subjectDnC: String? = null,
    /**
     * Subject O: Organization name
     * */
    val subjectDnO: String? = null,
    /**
     * Subject OU: Organization unit name
     * */
    val subjectDnOU: String? = null,
)
