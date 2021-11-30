package info.skyblond.archivedag.config;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.security.authentication.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Configuration
public class GRpcSecurityConfig {

    private final UserDetailsService userDetailsService;

    public GRpcSecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    AuthenticationManager authenticationManager() {
        final List<AuthenticationProvider> providers = new ArrayList<>();
        // let users' x509 cert provide their identity
        providers.add(new X509CertificateAuthenticationProvider(usernameExtractor(), this.userDetailsService));
        return new ProviderManager(providers);
    }

    @Bean
    public Function<X509CertificateAuthentication, String> usernameExtractor(){
        return authentication -> {
            // take the serial number of the cert and may check the revoke info of it.
            log.info("X509 serial no.: {}", authentication.getCredentials().getSerialNumber());
            // you can return null for non-validate serial number
            if (false) return null;
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
}
