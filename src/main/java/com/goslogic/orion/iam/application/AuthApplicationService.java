package com.goslogic.orion.iam.application;

import com.goslogic.orion.iam.application.exception.AuthException;
import com.goslogic.orion.iam.domain.model.LoginLog;
import com.goslogic.orion.iam.domain.model.LoginStatus;
import com.goslogic.orion.iam.domain.model.User;
import com.goslogic.orion.iam.domain.repository.LoginLogRepository;
import com.goslogic.orion.iam.domain.repository.UserRepository;
import com.goslogic.orion.iam.infrastructure.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class AuthApplicationService {

    private final UserRepository userRepository;
    private final LoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthApplicationService(UserRepository userRepository,
                                  LoginLogRepository loginLogRepository,
                                  PasswordEncoder passwordEncoder,
                                  JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.loginLogRepository = loginLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public record LoginResult(String userId, String tenantId, String driverId,
                              String accessToken, String expiresAt, String email) {}

    /**
     * Login genérico (SPA / gestor de flota). Acepta cualquier rol activo.
     */
    public LoginResult login(String email, String password, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Credenciales inválidas"));
        return authenticate(user, password, ipAddress, userAgent, null);
    }

    /**
     * Login específico para conductores (app móvil). Exige rol DRIVER.
     */
    public LoginResult driverLogin(String email, String password, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Credenciales inválidas"));

        if (!user.hasRole("DRIVER")) {
            recordFailedLogin(user, ipAddress, userAgent);
            throw new AuthException("El usuario no tiene rol de conductor");
        }

        return authenticate(user, password, ipAddress, userAgent, "DRIVER");
    }

    /**
     * Renueva el access token si el token actual todavía es válido.
     */
    public LoginResult refresh(String bearerToken) {
        String token = stripBearer(bearerToken);
        if (!jwtTokenProvider.isValid(token)) {
            throw new AuthException("Token inválido o expirado");
        }
        Claims claims = jwtTokenProvider.validateAndExtract(token);
        String userId = claims.getSubject();

        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        String newToken = jwtTokenProvider.generateToken(user);
        return buildResult(user, newToken);
    }

    /**
     * Logout stateless: registra el evento en login_logs.
     * El token continúa siendo técnicamente válido hasta su expiración.
     */
    public void logout(String bearerToken) {
        try {
            String token = stripBearer(bearerToken);
            Claims claims = jwtTokenProvider.validateAndExtract(token);
            userRepository.findById(Long.valueOf(claims.getSubject()))
                    .ifPresent(user -> loginLogRepository.save(
                            new LoginLog(user, user.getTenant(), null, "logout", LoginStatus.SUCCESS)));
        } catch (Exception ignored) {
            // logout nunca falla desde la perspectiva del cliente
        }
    }

    // ---- helpers privados ----

    private LoginResult authenticate(User user, String password,
                                     String ipAddress, String userAgent,
                                     String requiredRole) {
        if (!user.isActive()) {
            throw new AuthException("Cuenta inactiva");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            recordFailedLogin(user, ipAddress, userAgent);
            throw new AuthException("Credenciales inválidas");
        }

        loginLogRepository.save(
                new LoginLog(user, user.getTenant(), ipAddress, userAgent, LoginStatus.SUCCESS));

        String token = jwtTokenProvider.generateToken(user);
        return buildResult(user, token);
    }

    private LoginResult buildResult(User user, String token) {
        String expiresAt = Instant.now()
                .plusMillis(jwtTokenProvider.getExpirationMs())
                .truncatedTo(ChronoUnit.SECONDS)
                .toString();

        String driverId = user.hasRole("DRIVER") ? user.getExternalId() : null;

        return new LoginResult(
                user.getExternalId(),
                user.getTenant().getExternalId(),
                driverId,
                token,
                expiresAt,
                user.getEmail()
        );
    }

    private void recordFailedLogin(User user, String ipAddress, String userAgent) {
        loginLogRepository.save(
                new LoginLog(user, user.getTenant(), ipAddress, userAgent, LoginStatus.FAILED));
    }

    private String stripBearer(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return header;
    }
}
