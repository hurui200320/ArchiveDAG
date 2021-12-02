package info.skyblond.archivedag.security;

import info.skyblond.archivedag.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Component
public class JwtTokenManager {
    private final JwtProperties jwtProperties;
    private final Key signingKey;

    public JwtTokenManager(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        if (jwtProperties.getSecret() == null) {
            log.info("No JWT secret found, generating one...");
            this.signingKey = Keys.secretKeyFor(jwtProperties.getSignatureAlgorithm());
            log.info("Generated JWT secret key: {}", Base64.getEncoder().encodeToString(this.signingKey.getEncoded()));
        } else {
            this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.getSecret()));
        }
    }

    public String generateToken(UserDetails userDetails) {
        long currentTime = System.currentTimeMillis();
        long duration = this.jwtProperties.getExpireInUnit().toMillis(this.jwtProperties.getExpireInDuration());

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(currentTime))
                .setNotBefore(new Date(currentTime))
                .setExpiration(new Date(currentTime + duration))
                .signWith(this.signingKey, this.jwtProperties.getSignatureAlgorithm())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(this.signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        log.info("Get claims: {}", claims);
        Date now = new Date();
        if (claims.getNotBefore() != null && now.before(claims.getNotBefore())) {
            // now ... ntb
            return null;
        }
        // Here we reject token with no expiration
        if (Objects.requireNonNull(claims.getExpiration()).before(now)) {
            // exp ... now
            return null;
        }
        return claims.getSubject();
    }

}
