package com.goslogic.orion.iam.presentation.rest;

import com.goslogic.orion.iam.application.AuthApplicationService;
import com.goslogic.orion.iam.application.AuthApplicationService.LoginResult;
import com.goslogic.orion.iam.presentation.dto.LoginRequest;
import com.goslogic.orion.iam.presentation.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Auth", description = "Autenticación y gestión de sesión")
public class AuthController {

    private final AuthApplicationService authService;

    public AuthController(AuthApplicationService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login genérico (SPA / gestor de flota)")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req,
                                               HttpServletRequest httpReq) {
        LoginResult result = authService.login(req.email(), req.password(),
                httpReq.getRemoteAddr(), httpReq.getHeader("User-Agent"));

        return ResponseEntity.ok(LoginResponse.forWeb(result.userId(), result.tenantId(),
                result.accessToken(), result.expiresAt(), result.email()));
    }

    @PostMapping("/driver/login")
    @Operation(summary = "Login de conductor (app móvil Orion Driver)")
    public ResponseEntity<LoginResponse> driverLogin(@Valid @RequestBody LoginRequest req,
                                                     HttpServletRequest httpReq) {
        LoginResult result = authService.driverLogin(req.email(), req.password(),
                httpReq.getRemoteAddr(), httpReq.getHeader("User-Agent"));

        return ResponseEntity.ok(LoginResponse.forDriver(result.driverId(), result.tenantId(),
                result.accessToken(), result.expiresAt(), result.email()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token")
    public ResponseEntity<LoginResponse> refresh(
            @RequestHeader("Authorization") String authorization) {
        LoginResult result = authService.refresh(authorization);
        String driverId = result.driverId();
        if (driverId != null) {
            return ResponseEntity.ok(LoginResponse.forDriver(driverId, result.tenantId(),
                    result.accessToken(), result.expiresAt(), result.email()));
        }
        return ResponseEntity.ok(LoginResponse.forWeb(result.userId(), result.tenantId(),
                result.accessToken(), result.expiresAt(), result.email()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión (registra evento; JWT sigue válido hasta expiración)")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
    }
}
