package com.goslogic.orion.iam.presentation.rest;

import com.goslogic.orion.iam.application.TenantApplicationService;
import com.goslogic.orion.iam.domain.model.Tenant;
import com.goslogic.orion.iam.presentation.dto.CreateTenantRequest;
import com.goslogic.orion.iam.presentation.dto.TenantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenants")
@Tag(name = "Tenants", description = "Gestión de empresas (tenants) — requiere rol ADMIN")
public class TenantController {

    private final TenantApplicationService tenantService;

    public TenantController(TenantApplicationService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @Operation(summary = "Onboarding: crear tenant + usuario ADMIN inicial")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest req) {
        Tenant tenant = tenantService.createTenant(new TenantApplicationService.CreateTenantRequest(
                req.name(), req.ruc(), req.adminEmail(), req.adminPassword(),
                req.adminFirstName(), req.adminLastName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant));
    }

    @GetMapping
    @Operation(summary = "Listar todos los tenants")
    public ResponseEntity<List<TenantResponse>> listTenants() {
        List<TenantResponse> result = tenantService.findAll().stream()
                .map(TenantResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar tenant por external_id")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable String id) {
        return ResponseEntity.ok(TenantResponse.from(tenantService.findByExternalId(id)));
    }
}
