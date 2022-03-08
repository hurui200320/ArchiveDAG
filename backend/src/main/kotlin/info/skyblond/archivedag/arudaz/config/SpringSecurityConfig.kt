package info.skyblond.archivedag.arudaz.config

import info.skyblond.archivedag.arstue.CertService
import info.skyblond.archivedag.arudaz.security.JWTAuthenticationEntryPoint
import info.skyblond.archivedag.arudaz.security.JwtRequestFilter
import net.devh.boot.grpc.server.security.authentication.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.util.function.Function

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class SpringSecurityConfig(
    private val entryPoint: JWTAuthenticationEntryPoint,
    private val jwtRequestFilter: JwtRequestFilter,
    private val userDetailsService: UserDetailsService,
    private val certService: CertService
) : WebSecurityConfigurerAdapter() {

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder())
            .and() // don't forget the gRPC auth provider
            .authenticationProvider(
                X509CertificateAuthenticationProvider(x509UsernameExtractor(), userDetailsService)
            )
    }

    // Use CN from x509 cert as username
    @Bean
    fun x509UsernameExtractor(): Function<X509CertificateAuthentication, String?> {
        return Function { authentication: X509CertificateAuthentication ->
            val serialNumber = authentication.credentials.serialNumber.toString(16)
            val username = X509CertificateAuthenticationProvider.CN_USERNAME_EXTRACTOR.apply(authentication)
            try {
                certService.verifyCertStatus(serialNumber, username)
            } catch (e: AuthenticationException) {
                return@Function null
            }
            username
        }
    }

    @Bean
    fun grpcAuthenticationReader(): GrpcAuthenticationReader {
        val readers: MutableList<GrpcAuthenticationReader> = ArrayList()
        // read user details from his/her/their x509 certs
        readers.add(SSLContextGrpcAuthenticationReader())
        return CompositeGrpcAuthenticationReader(readers)
    }

    // Expose the Authentication manager bean
    @Bean
    override fun authenticationManagerBean(): AuthenticationManager {
        return super.authenticationManagerBean()
    }

    override fun configure(http: HttpSecurity) {
        // disable CSRF and CORS for REST api
        http.cors().and().csrf().disable()
        // Stateless session for JWT
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        // override the default 401 redirect
        http.exceptionHandling().authenticationEntryPoint(entryPoint)
        // set endpoints
        http.authorizeRequests() // public controller
            .antMatchers("/public/**").permitAll() // rest of them
            .anyRequest().authenticated()
        // add our JWT filter
        http.addFilterBefore(
            jwtRequestFilter,
            UsernamePasswordAuthenticationFilter::class.java
        )
    }

    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowCredentials = true
        config.addAllowedOriginPattern("*")
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")
        source.registerCorsConfiguration("/**", config)
        return CorsFilter(source)
    }

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder()
    }

}
