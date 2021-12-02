package info.skyblond.archivedag.config;

import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

@Getter
@Setter
@ConfigurationProperties(value = "jwt")
public class JwtProperties {
    private String secret = null;
    private long expireInDuration = 5;
    private TimeUnit expireInUnit = TimeUnit.MINUTES;
    private SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS512;
}
