package info.skyblond.archivedag.arstue.config

import info.skyblond.archivedag.arstue.model.CertSigningInfo
import info.skyblond.archivedag.arstue.utils.readPrivateKey
import info.skyblond.archivedag.arstue.utils.readX509Cert
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate

@Configuration
@EnableConfigurationProperties(CertSigningProperties::class)
class CertSigningConfiguration(
    private val properties: CertSigningProperties
) {
    @Bean
    fun certSigningInfo(): CertSigningInfo {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        println("???" + properties.caPrivateKey.file.absolutePath)
        val caPrivateKey: PrivateKey = readPrivateKey(
            properties.caPrivateKey.readableChannel(),
            properties.caPrivateKeyPassword
        )
        val caCert: X509Certificate = readX509Cert(
            properties.caCert.readableChannel()
        )
        return CertSigningInfo(
            caPrivateKey = caPrivateKey,
            caCert = caCert,
            generatedKeySize = properties.generateKeySize,
            signAlgName = properties.signAlgName,
            customSubjectDN = properties.customSubjectDN,
            expireInDuration = properties.expireInDuration,
            expireInUnit = properties.expireInUnit
        )
    }
}
