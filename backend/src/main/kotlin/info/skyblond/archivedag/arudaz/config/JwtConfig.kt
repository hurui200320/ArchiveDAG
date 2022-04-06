package info.skyblond.archivedag.arudaz.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AccountStatusUserDetailsChecker

@Configuration
class JwtConfig {
    /**
     * For JWT token resolve. Check user status.
     * This cannot be merged into SpringSecurityConfig, it will be a loop.
     * */
    @Bean
    fun accountStatusUserDetailsChecker(): AccountStatusUserDetailsChecker {
        return AccountStatusUserDetailsChecker()
    }
}
