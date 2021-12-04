package info.skyblond.archivedag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@ConfigurationProperties(value = "cert-signing")
public class CertSigningProperties {
    private Resource caCert = null;
    private Resource caPrivateKey = null;
    private String caPrivateKeyPassword = "";
    private String customSubjectDN = "";
    private String signAlgName = "SHA3-512WITHRSA";
    private int generateKeySize = 4096;
    private long expireInDuration = 90;
    private TimeUnit expireInUnit = TimeUnit.DAYS;
}
