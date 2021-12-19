package info.skyblond.archivedag.arstue.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.Resource
import java.util.concurrent.TimeUnit

@ConstructorBinding
@ConfigurationProperties(value = "cert-signing")
data class CertSigningProperties(
    val caCert: Resource,
    val caPrivateKey: Resource,
    val caPrivateKeyPassword: String = "",
    val customSubjectDN: String = "",
    val signAlgName: String = "SHA512WITHRSA",
    val generateKeySize: Int = 4096,
    val expireInDuration: Long = 90,
    val expireInUnit: TimeUnit = TimeUnit.DAYS
)
