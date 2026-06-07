package com.goslogic.orion.iam.application;

import com.goslogic.orion.iam.application.exception.ConflictException;
import com.goslogic.orion.iam.application.exception.ResourceNotFoundException;
import com.goslogic.orion.iam.domain.model.Role;
import com.goslogic.orion.iam.domain.model.Tenant;
import com.goslogic.orion.iam.domain.model.User;
import com.goslogic.orion.iam.domain.repository.RoleRepository;
import com.goslogic.orion.iam.domain.repository.TenantRepository;
import com.goslogic.orion.iam.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;
    private UserApplicationService userService;

    private Tenant tenant;
    private User existingUser;
    private Role managerRole;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserApplicationService(userRepository, tenantRepository,
                roleRepository, passwordEncoder);

        tenant = new Tenant("tenant-demo", "Demo", null);
        setId(tenant, 1L);

        existingUser = new User("manager-demo", tenant,
                "gestor@empresa.com", passwordEncoder.encode("123456"),
                "Demo", "Gestor");
        setId(existingUser, 2L);
        managerRole = new Role("FLEET_MANAGER", "Gestor de flota");
        setId(managerRole, 3L);
        existingUser.getRoles().add(managerRole);
    }

    @Test
    void createUser_withValidData_returnsSavedUser() {
        var request = new UserApplicationService.CreateUserRequest(
                "tenant-demo", "nuevo@empresa.com", "123456",
                "María", "López", Set.of("FLEET_MANAGER"));

        when(tenantRepository.findByExternalId("tenant-demo")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmailAndTenantId("nuevo@empresa.com", 1L)).thenReturn(false);
        when(roleRepository.findByName("FLEET_MANAGER")).thenReturn(Optional.of(managerRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.createUser(request);

        assertThat(result.getEmail()).isEqualTo("nuevo@empresa.com");
        assertThat(result.getExternalId()).startsWith("user-");
        assertThat(result.getRoles()).contains(managerRole);
        assertThat(passwordEncoder.matches("123456", result.getPasswordHash())).isTrue();
    }

    @Test
    void createUser_whenTenantNotFound_throwsResourceNotFoundException() {
        var request = new UserApplicationService.CreateUserRequest(
                "tenant-inexistente", "nuevo@empresa.com", "123456", null, null, null);

        when(tenantRepository.findByExternalId("tenant-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tenant no encontrado");
    }

    @Test
    void createUser_whenEmailExists_throwsConflictException() {
        var request = new UserApplicationService.CreateUserRequest(
                "tenant-demo", "gestor@empresa.com", "123456", null, null, null);

        when(tenantRepository.findByExternalId("tenant-demo")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmailAndTenantId("gestor@empresa.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email ya registrado");
    }

    @Test
    void createUser_whenRoleNotFound_throwsResourceNotFoundException() {
        var request = new UserApplicationService.CreateUserRequest(
                "tenant-demo", "nuevo@empresa.com", "123456", null, null, Set.of("INVALID"));

        when(tenantRepository.findByExternalId("tenant-demo")).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmailAndTenantId("nuevo@empresa.com", 1L)).thenReturn(false);
        when(roleRepository.findByName("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rol no encontrado");
    }

    @Test
    void findAllByTenant_returnsUsersForTenant() {
        when(tenantRepository.findByExternalId("tenant-demo")).thenReturn(Optional.of(tenant));
        when(userRepository.findAllByTenantId(1L)).thenReturn(List.of(existingUser));

        List<User> result = userService.findAllByTenant("tenant-demo");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("gestor@empresa.com");
    }

    @Test
    void findByExternalId_whenExists_returnsUser() {
        when(userRepository.findByExternalId("manager-demo")).thenReturn(Optional.of(existingUser));

        User result = userService.findByExternalId("manager-demo");

        assertThat(result.getExternalId()).isEqualTo("manager-demo");
    }

    @Test
    void findByExternalId_whenNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByExternalId("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByExternalId("inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void assignRoles_replacesExistingRoles() {
        Role driverRole = new Role("DRIVER", "Conductor");
        setId(driverRole, 4L);

        when(userRepository.findByExternalId("manager-demo")).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("DRIVER")).thenReturn(Optional.of(driverRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.assignRoles("manager-demo", Set.of("DRIVER"));

        assertThat(result.getRoles()).containsExactly(driverRole);
        assertThat(result.getRoles()).doesNotContain(managerRole);
    }

    @Test
    void assignRoles_whenRoleNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByExternalId("manager-demo")).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.assignRoles("manager-demo", Set.of("INVALID")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rol no encontrado");
    }

    @Test
    void deactivateUser_setsActiveFalse() {
        when(userRepository.findByExternalId("manager-demo")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deactivateUser("manager-demo");

        assertThat(existingUser.isActive()).isFalse();
        verify(userRepository).save(existingUser);
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
