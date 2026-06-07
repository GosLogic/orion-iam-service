package com.goslogic.orion.iam.application;

import com.goslogic.orion.iam.application.exception.ConflictException;
import com.goslogic.orion.iam.application.exception.ResourceNotFoundException;
import com.goslogic.orion.iam.domain.model.Role;
import com.goslogic.orion.iam.domain.model.Tenant;
import com.goslogic.orion.iam.domain.model.User;
import com.goslogic.orion.iam.domain.repository.RoleRepository;
import com.goslogic.orion.iam.domain.repository.TenantRepository;
import com.goslogic.orion.iam.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UserApplicationService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserApplicationService(UserRepository userRepository,
                                  TenantRepository tenantRepository,
                                  RoleRepository roleRepository,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record CreateUserRequest(String tenantExternalId, String email, String password,
                                    String firstName, String lastName, Set<String> roleNames) {}

    /**
     * Crea un usuario dentro del tenant especificado.
     * El tenant se extrae del JWT inyectado por el gateway (X-Tenant-Id header).
     */
    public User createUser(CreateUserRequest req) {
        Tenant tenant = tenantRepository.findByExternalId(req.tenantExternalId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + req.tenantExternalId()));

        if (userRepository.existsByEmailAndTenantId(req.email(), tenant.getId())) {
            throw new ConflictException("Email ya registrado en este tenant: " + req.email());
        }

        String externalId = "user-" + UUID.randomUUID().toString().substring(0, 8);
        User user = new User(externalId, tenant, req.email(),
                passwordEncoder.encode(req.password()),
                req.firstName(), req.lastName());

        if (req.roleNames() != null) {
            for (String roleName : req.roleNames()) {
                Role role = roleRepository.findByName(roleName.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName));
                user.getRoles().add(role);
            }
        }

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAllByTenant(String tenantExternalId) {
        Tenant tenant = tenantRepository.findByExternalId(tenantExternalId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado: " + tenantExternalId));
        return userRepository.findAllByTenantId(tenant.getId());
    }

    @Transactional(readOnly = true)
    public User findByExternalId(String externalId) {
        return userRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + externalId));
    }

    public User assignRoles(String userExternalId, Set<String> roleNames) {
        User user = findByExternalId(userExternalId);
        user.getRoles().clear();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + roleName));
            user.getRoles().add(role);
        }
        return userRepository.save(user);
    }

    public void deactivateUser(String externalId) {
        User user = findByExternalId(externalId);
        user.setActive(false);
        userRepository.save(user);
    }
}
