package info.skyblond.archivedag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@Getter
@Setter
@ConfigurationProperties(value = "cert-signing")
public class CertSigningProperties {
    private Resource caCert = null;
    private Resource caPrivateKey = null;
    private String caPrivateKeyPassword = "";
    private int generateKeySize = 4096;
}
