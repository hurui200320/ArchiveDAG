package info.skyblond.archivedag.config;

import info.skyblond.archivedag.security.JwtRequestFilter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.security.authentication.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Configuration
@EnableWebSecurity
// This annotation can be used on both GRpcSecurityConfig and this config.
// But can only occur once. I'd prefer here.
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    private final AuthenticationEntryPoint entryPoint;
    private final JwtRequestFilter jwtRequestFilter;
    private final UserDetailsService userDetailsService;

    public SpringSecurityConfig(AuthenticationEntryPoint entryPoint, JwtRequestFilter jwtRequestFilter, UserDetailsService userDetailsService) {
        this.entryPoint = entryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(this.userDetailsService)
                .passwordEncoder(this.passwordEncoder())
                .and()
                // don't forget the gRPC auth provider
                .authenticationProvider(
                        new X509CertificateAuthenticationProvider(this.x509UsernameExtractor(), this.userDetailsService)
                );
    }

    @Bean
    public Function<X509CertificateAuthentication, String> x509UsernameExtractor() {
        return authentication -> {
            // take the serial number of the cert and may check the revoke info of it.
            log.info("X509 serial no.: {}", authentication.getCredentials().getSerialNumber());
            // TODO you can return null for non-validate serial number
            if (false) {
                return null;
            }
            // then extract the username from subject CN
            return X509CertificateAuthenticationProvider.CN_USERNAME_EXTRACTOR.apply(authentication);
        };
    }

    @Bean
    GrpcAuthenticationReader grpcAuthenticationReader() {
        final List<GrpcAuthenticationReader> readers = new ArrayList<>();
        // read user details from his/her/their x509 certs
        readers.add(new SSLContextGrpcAuthenticationReader());
        return new CompositeGrpcAuthenticationReader(readers);
    }

    // Expose the Authentication manager bean
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // disable CSRF and CORS for REST api
        http.cors().and().csrf().disable();
        // Stateless session for JWT
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        // override the default 401 redirect
        http.exceptionHandling().authenticationEntryPoint(this.entryPoint);
        // set endpoints
        http.authorizeRequests()
                .antMatchers("/public/**").permitAll()
                .anyRequest().authenticated();
        // add our JWT filter
        http.addFilterBefore(this.jwtRequestFilter,
                UsernamePasswordAuthenticationFilter.class);
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
