package com.goslogic.orion.iam.application;

import com.goslogic.orion.iam.application.exception.ConflictException;
import com.goslogic.orion.iam.application.exception.ResourceNotFoundException;
import com.goslogic.orion.iam.domain.model.Role;
import com.goslogic.orion.iam.domain.model.Tenant;
import com.goslogic.orion.iam.domain.model.TenantStatus;
import com.goslogic.orion.iam.domain.model.User;
import com.goslogic.orion.iam.domain.repository.RoleRepository;
import com.goslogic.orion.iam.domain.repository.TenantRepository;
import com.goslogic.orion.iam.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TenantApplicationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantApplicationService(TenantRepository tenantRepository,
                                    UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record CreateTenantRequest(String name, String ruc,
                                      String adminEmail, String adminPassword,
                                      String adminFirstName, String adminLastName) {}

    /**
     * Onboarding: crea el tenant + usuario ADMIN inicial.
     */
    public Tenant createTenant(CreateTenantRequest req) {
        if (req.ruc() != null && tenantRepository.existsByRuc(req.ruc())) {
            throw new ConflictException("Ya existe un tenant con ese RUC: " + req.ruc());
        }

        String externalId = "tenant-" + UUID.randomUUID().toString().substring(0, 8);
        Tenant tenant = new Tenant(externalId, req.name(), req.ruc());
        tenantRepository.save(tenant);

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado — ejecutar DataSeeder primero"));

        if (userRepository.existsByEmailAndTenantId(req.adminEmail(), tenant.getId())) {
            throw new ConflictException("El email ya está registrado en este tenant");
        }

        String userExternalId = "user-" + UUID.randomUUID().toString().substring(0, 8);
        User admin = new User(userExternalId, tenant, req.adminEmail(),
                passwordEncoder.encode(req.adminPassword()),
                req.adminFirstName(), req.adminLastName());
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        return tenant;
    }

    @Transactional(readOnly = true)
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tenant findByExternalId(String externalId) {
        return tenantRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + externalId));
    }

    public void deactivateTenant(String externalId) {
        Tenant tenant = findByExternalId(externalId);
        tenant.setStatus(TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
    }
}
