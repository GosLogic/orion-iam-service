package com.goslogic.orion.iam.application;

import com.goslogic.orion.iam.application.exception.AuthException;
import com.goslogic.orion.iam.domain.model.Role;
import com.goslogic.orion.iam.domain.model.Tenant;
import com.goslogic.orion.iam.domain.model.User;
import com.goslogic.orion.iam.domain.repository.LoginLogRepository;
import com.goslogic.orion.iam.domain.repository.UserRepository;
import com.goslogic.orion.iam.infrastructure.security.JwtProperties;
import com.goslogic.orion.iam.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthApplicationService authService;

    private User driverUser;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-for-unit-tests-only-min-256-bits-padding");
        props.setExpirationMs(3_600_000L);

        passwordEncoder = new BCryptPasswordEncoder();
        jwtTokenProvider = new JwtTokenProvider(props);
        authService = new AuthApplicationService(userRepository, loginLogRepository,
                passwordEncoder, jwtTokenProvider);

        Tenant tenant = new Tenant("tenant-demo", "Demo", null);
        setId(tenant, 1L);

        driverUser = new User("driver-demo", tenant,
                "conductor@empresa.com",
                passwordEncoder.encode("123456"),
                "Demo", "Driver");
        setId(driverUser, 1L);

        Role driverRole = new Role("DRIVER", "Driver role");
        setId(driverRole, 1L);
        driverUser.getRoles().add(driverRole);

        when(loginLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void driverLogin_withValidCredentials_returnsLoginResult() {
        when(userRepository.findByEmail("conductor@empresa.com"))
                .thenReturn(Optional.of(driverUser));

        AuthApplicationService.LoginResult result =
                authService.driverLogin("conductor@empresa.com", "123456", "127.0.0.1", "test");

        assertThat(result.driverId()).isEqualTo("driver-demo");
        assertThat(result.tenantId()).isEqualTo("tenant-demo");
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.expiresAt()).isNotBlank();
    }

    @Test
    void driverLogin_withWrongPassword_throwsAuthException() {
        when(userRepository.findByEmail("conductor@empresa.com"))
                .thenReturn(Optional.of(driverUser));

        assertThatThrownBy(() ->
                authService.driverLogin("conductor@empresa.com", "wrong", "127.0.0.1", "test"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Credenciales");
    }

    @Test
    void driverLogin_withNonDriverRole_throwsAuthException() {
        driverUser.getRoles().clear();
        Role managerRole = new Role("FLEET_MANAGER", "Manager");
        setId(managerRole, 2L);
        driverUser.getRoles().add(managerRole);

        when(userRepository.findByEmail("conductor@empresa.com"))
                .thenReturn(Optional.of(driverUser));

        assertThatThrownBy(() ->
                authService.driverLogin("conductor@empresa.com", "123456", "127.0.0.1", "test"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("conductor");
    }

    @Test
    void driverLogin_withUnknownEmail_throwsAuthException() {
        when(userRepository.findByEmail("unknown@empresa.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.driverLogin("unknown@empresa.com", "123456", "127.0.0.1", "test"))
                .isInstanceOf(AuthException.class);
    }

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
