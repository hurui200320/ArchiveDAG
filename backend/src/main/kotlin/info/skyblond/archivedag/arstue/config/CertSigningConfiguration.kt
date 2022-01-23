package info.skyblond.archivedag.arstue.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
@EnableConfigurationProperties(CertSigningProperties::class)
class CertSigningConfiguration(
    private val properties: CertSigningProperties
) {
    private val logger = LoggerFactory.getLogger(CertSigningConfiguration::class.java)

    @PostConstruct
    fun postConstruct() {
        if (!properties.subjectDnC.isNullOrBlank()) {
            logger.info("Custom subject dn for C: ${properties.subjectDnC}")
        }
        if (!properties.subjectDnO.isNullOrBlank()) {
            logger.info("Custom subject dn for O: ${properties.subjectDnO}")
        }
        if (!properties.subjectDnOU.isNullOrBlank()) {
            logger.info("Custom subject dn for OU: ${properties.subjectDnOU}")
        }
    }
}
