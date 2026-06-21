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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantApplicationServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;
    private TenantApplicationService tenantService;

    private Role adminRole;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        tenantService = new TenantApplicationService(tenantRepository, userRepository,
                roleRepository, passwordEncoder);

        adminRole = new Role("ADMIN", "Administrador");
        setId(adminRole, 1L);
    }

    @Test
    void createTenant_withValidData_createsTenantAndAdmin() {
        var request = new TenantApplicationService.CreateTenantRequest(
                "Transportes Lima SAC", "20987654321",
                "admin@transportes.com", "admin123", "Carlos", "García");

        when(tenantRepository.existsByRuc("20987654321")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            setId(t, 10L);
            return t;
        });
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.existsByEmailAndTenantId("admin@transportes.com", 10L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = tenantService.createTenant(request);

        assertThat(result.getName()).isEqualTo("Transportes Lima SAC");
        assertThat(result.getRuc()).isEqualTo("20987654321");
        assertThat(result.getExternalId()).startsWith("tenant-");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedAdmin = userCaptor.getValue();
        assertThat(savedAdmin.getEmail()).isEqualTo("admin@transportes.com");
        assertThat(savedAdmin.getRoles()).contains(adminRole);
        assertThat(passwordEncoder.matches("admin123", savedAdmin.getPasswordHash())).isTrue();
    }

    @Test
    void createTenant_withDuplicateRuc_throwsConflictException() {
        var request = new TenantApplicationService.CreateTenantRequest(
                "Empresa Duplicada", "20123456789",
                "admin@dup.com", "admin123", null, null);

        when(tenantRepository.existsByRuc("20123456789")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RUC");
    }

    @Test
    void findByExternalId_whenExists_returnsTenant() {
        Tenant tenant = new Tenant("tenant-demo", "Demo", "20123456789");
        when(tenantRepository.findByExternalId("tenant-demo")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.findByExternalId("tenant-demo");

        assertThat(result.getExternalId()).isEqualTo("tenant-demo");
        assertThat(result.getName()).isEqualTo("Demo");
    }

    @Test
    void findByExternalId_whenNotFound_throwsResourceNotFoundException() {
        when(tenantRepository.findByExternalId("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.findByExternalId("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tenant no encontrado");
    }

    @Test
    void findAll_returnsTenantList() {
        Tenant t1 = new Tenant("tenant-demo", "Demo", null);
        Tenant t2 = new Tenant("tenant-abc", "Otra", null);
        when(tenantRepository.findAll()).thenReturn(List.of(t1, t2));

        List<Tenant> result = tenantService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void deactivateTenant_setsStatusInactive() {
        Tenant tenant = new Tenant("tenant-demo", "Demo", null);
        when(tenantRepository.findByExternalId("tenant-demo")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        tenantService.deactivateTenant("tenant-demo");

        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.INACTIVE);
        verify(tenantRepository).save(tenant);
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
