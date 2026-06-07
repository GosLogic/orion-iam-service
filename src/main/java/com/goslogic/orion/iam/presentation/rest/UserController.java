package com.goslogic.orion.iam.presentation.rest;

import com.goslogic.orion.iam.application.UserApplicationService;
import com.goslogic.orion.iam.domain.model.User;
import com.goslogic.orion.iam.presentation.dto.AssignRolesRequest;
import com.goslogic.orion.iam.presentation.dto.CreateUserRequest;
import com.goslogic.orion.iam.presentation.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "Gestión de usuarios dentro de un tenant")
public class UserController {

    private final UserApplicationService userService;

    public UserController(UserApplicationService userService) {
        this.userService = userService;
    }

    /**
     * Crea un usuario en el tenant indicado por el header X-Tenant-Id (inyectado por el Gateway).
     * El administrador solo puede crear usuarios en su propio tenant.
     */
    @PostMapping
    @Operation(summary = "Crear usuario en el tenant del caller")
    public ResponseEntity<UserResponse> createUser(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Valid @RequestBody CreateUserRequest req) {

        String effectiveTenantId = (tenantId != null) ? tenantId : req.email();
        User user = userService.createUser(new UserApplicationService.CreateUserRequest(
                effectiveTenantId, req.email(), req.password(),
                req.firstName(), req.lastName(), req.roles()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios del tenant del caller")
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestHeader(value = "X-Tenant-Id", required = true) String tenantId) {
        List<UserResponse> result = userService.findAllByTenant(tenantId).stream()
                .map(UserResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuario por external_id")
    public ResponseEntity<UserResponse> getUser(@PathVariable String id) {
        return ResponseEntity.ok(UserResponse.from(userService.findByExternalId(id)));
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "Asignar (reemplazar) roles del usuario")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable String id,
                                                    @Valid @RequestBody AssignRolesRequest req) {
        User updated = userService.assignRoles(id, req.roles());
        return ResponseEntity.ok(UserResponse.from(updated));
    }
}
