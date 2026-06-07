package com.goslogic.orion.iam.infrastructure.security;

import com.goslogic.orion.iam.domain.model.Role;
import com.goslogic.orion.iam.domain.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Genera y valida JWT firmados con HMAC-SHA256.
 *
 * Claims emitidos (compartidos con el API Gateway):
 *   sub          → user.id (Long, como String)
 *   email        → user.email
 *   tenant_id    → tenant.externalId  (ej. "tenant-demo")
 *   tenant_pk    → tenant.id (Long)   (para resolución interna)
 *   driver_id    → user.externalId si tiene rol DRIVER (ej. "driver-demo")
 *   roles        → lista de nombres de roles
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.signingKey = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(props.getExpirationMs());

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        var builder = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("tenant_id", user.getTenant().getExternalId())
                .claim("tenant_pk", user.getTenant().getId())
                .claim("roles", roleNames)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey);

        if (user.hasRole("DRIVER")) {
            builder.claim("driver_id", user.getExternalId());
        }

        return builder.compact();
    }

    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            validateAndExtract(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMs() {
        return props.getExpirationMs();
    }
}
