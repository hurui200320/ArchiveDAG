package info.skyblond.archivedag.config;

import info.skyblond.archivedag.model.bo.CertSigningInfo;
import info.skyblond.archivedag.util.GeneralKt;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Configuration
@EnableConfigurationProperties(CertSigningProperties.class)
public class CertSigningConfig {
    private final CertSigningProperties properties;

    public CertSigningConfig(CertSigningProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CertSigningInfo certSigningInfo() throws IOException {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        PrivateKey caPrivateKey = GeneralKt.readPrivateKey(
                this.properties.getCaPrivateKey().readableChannel(),
                this.properties.getCaPrivateKeyPassword());
        X509Certificate caCert = GeneralKt.readX509Cert(
                this.properties.getCaCert().readableChannel());

        return new CertSigningInfo(caPrivateKey, caCert,
                this.properties.getGenerateKeySize(),
                this.properties.getSignAlgName(),
                this.properties.getCustomSubjectDN(),
                this.properties.getExpireInDuration(),
                this.properties.getExpireInUnit());
    }
}

