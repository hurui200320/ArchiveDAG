package info.skyblond.archivedag.arudaz.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AccountStatusUserDetailsChecker

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfiguration {
    @Bean
    fun accountStatusUserDetailsChecker(): AccountStatusUserDetailsChecker {
        return AccountStatusUserDetailsChecker()
    }
}
