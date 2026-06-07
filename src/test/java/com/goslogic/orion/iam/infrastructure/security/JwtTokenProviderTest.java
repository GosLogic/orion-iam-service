package com.goslogic.orion.iam.infrastructure.security;

import com.goslogic.orion.iam.domain.model.Role;
import com.goslogic.orion.iam.domain.model.Tenant;
import com.goslogic.orion.iam.domain.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-unit-tests-only-min-256-bits-padding");
        props.setExpirationMs(3_600_000L); // 1 hora
        provider = new JwtTokenProvider(props);
    }

    private User buildUser(boolean isDriver) {
        Tenant tenant = new Tenant("tenant-demo", "Demo Tenant", null);
        // Simular IDs sin persistencia
        setId(tenant, 1L);

        User user = new User("driver-demo", tenant,
                "conductor@empresa.com", "hash", "Demo", "User");
        setId(user, 42L);

        Role role = new Role(isDriver ? "DRIVER" : "FLEET_MANAGER", "desc");
        setId(role, 1L);
        user.getRoles().add(role);
        return user;
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        User user = buildUser(true);
        String token = provider.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void validateAndExtract_returnsCorrectClaims() {
        User user = buildUser(true);
        String token = provider.generateToken(user);

        Claims claims = provider.validateAndExtract(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("tenant_id", String.class)).isEqualTo("tenant-demo");
        assertThat(claims.get("driver_id", String.class)).isEqualTo("driver-demo");
        assertThat(claims.get("email", String.class)).isEqualTo("conductor@empresa.com");

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).contains("DRIVER");
    }

    @Test
    void generateToken_noDriverIdWhenNotDriver() {
        User user = buildUser(false);
        String token = provider.generateToken(user);

        Claims claims = provider.validateAndExtract(token);
        assertThat(claims.get("driver_id")).isNull();
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        User user = buildUser(true);
        String token = provider.generateToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(provider.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("test-secret-key-for-unit-tests-only-min-256-bits-padding");
        shortProps.setExpirationMs(1L); // expira inmediatamente
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);

        User user = buildUser(true);
        String token = shortProvider.generateToken(user);

        // Esperar 10ms para asegurar expiración
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(shortProvider.isValid(token)).isFalse();
    }

    // Reflexión auxiliar para setear IDs en entidades sin persistencia
    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
