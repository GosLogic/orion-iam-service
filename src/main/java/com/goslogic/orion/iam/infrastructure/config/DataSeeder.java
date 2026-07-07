package com.goslogic.orion.iam.infrastructure.config;

import com.goslogic.orion.iam.domain.model.*;
import com.goslogic.orion.iam.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga datos demo al arrancar (excluido en perfil "test").
 *
 * Crea:
 *   - Roles: ADMIN, FLEET_MANAGER, DRIVER
 *   - Permissions: FLEET/TRIPS/ALERTS (READ, WRITE, MASTER para cada rol relevante)
 *   - Tenant demo: external_id="tenant-demo"
 *   - Conductor demo: conductor@empresa.com / 123456 · external_id="driver-demo" · rol DRIVER
 *   - Gestor demo: gestor@empresa.com / 123456 · external_id="manager-demo" · rol FLEET_MANAGER
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoleRepository roleRepository,
                      PermissionRepository permissionRepository,
                      TenantRepository tenantRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRolesAndPermissions();
        seedDemoTenantAndUsers();
        seedBetaTenantAndUsers();
    }

    private void seedRolesAndPermissions() {
        // --- Permissions ---
        Permission fleetRead   = getOrCreatePermission("FLEET",  AccessLevel.READ);
        Permission fleetWrite  = getOrCreatePermission("FLEET",  AccessLevel.WRITE);
        Permission fleetMaster = getOrCreatePermission("FLEET",  AccessLevel.MASTER);
        Permission tripsRead   = getOrCreatePermission("TRIPS",  AccessLevel.READ);
        Permission tripsWrite  = getOrCreatePermission("TRIPS",  AccessLevel.WRITE);
        Permission tripsMaster = getOrCreatePermission("TRIPS",  AccessLevel.MASTER);
        Permission alertsRead  = getOrCreatePermission("ALERTS", AccessLevel.READ);
        Permission alertsWrite = getOrCreatePermission("ALERTS", AccessLevel.WRITE);
        Permission alertsMaster= getOrCreatePermission("ALERTS", AccessLevel.MASTER);

        // --- Roles ---
        Role admin = getOrCreateRole("ADMIN", "Administrador del tenant — acceso total");
        admin.getPermissions().addAll(java.util.Set.of(
                fleetMaster, tripsMaster, alertsMaster));
        roleRepository.save(admin);

        Role fleetManager = getOrCreateRole("FLEET_MANAGER",
                "Gestor de flota — puede gestionar rutas y monitorear");
        fleetManager.getPermissions().addAll(java.util.Set.of(
                fleetRead, fleetWrite, tripsRead, tripsWrite, alertsRead));
        roleRepository.save(fleetManager);

        Role driver = getOrCreateRole("DRIVER", "Conductor — acceso de lectura y reporte de incidentes");
        driver.getPermissions().addAll(java.util.Set.of(
                tripsRead, tripsWrite, alertsWrite));
        roleRepository.save(driver);

        log.info("[DataSeeder] Roles y permisos listos");
    }

    private void seedDemoTenantAndUsers() {
        if (tenantRepository.existsByExternalId("tenant-demo")) {
            log.info("[DataSeeder] Datos demo ya existentes — omitiendo seed");
            return;
        }

        // Tenant
        Tenant tenant = new Tenant("tenant-demo", "Empresa Demo Orion", "20123456789");
        tenantRepository.save(tenant);

        Role driverRole = roleRepository.findByName("DRIVER")
                .orElseThrow(() -> new IllegalStateException("Rol DRIVER no encontrado"));
        Role managerRole = roleRepository.findByName("FLEET_MANAGER")
                .orElseThrow(() -> new IllegalStateException("Rol FLEET_MANAGER no encontrado"));
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado"));

        // Conductor demo — compatible con el contrato móvil (movil.md §6.5)
        User conductor = new User("driver-demo", tenant,
                "conductor@empresa.com",
                passwordEncoder.encode("123456"),
                "Demo", "Conductor");
        conductor.getRoles().add(driverRole);
        userRepository.save(conductor);

        // Gestor de flota demo
        User gestor = new User("manager-demo", tenant,
                "gestor@empresa.com",
                passwordEncoder.encode("123456"),
                "Demo", "Gestor");
        gestor.getRoles().add(managerRole);
        userRepository.save(gestor);

        // Admin demo
        User admin = new User("admin-demo", tenant,
                "admin@empresa.com",
                passwordEncoder.encode("admin123"),
                "Demo", "Admin");
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        log.info("[DataSeeder] Datos demo creados: tenant-demo");
        log.info("[DataSeeder] Credenciales demo — gestor@empresa.com/123456 (FLEET_MANAGER), admin@empresa.com/admin123 (ADMIN), conductor@empresa.com/123456 (DRIVER)");
    }

    /** Segundo tenant para la demo de aislamiento multi-tenant (P0-3). Guard propio. */
    private void seedBetaTenantAndUsers() {
        if (tenantRepository.existsByExternalId("tenant-beta")) {
            log.info("[DataSeeder] Tenant beta ya existente — omitiendo seed");
            return;
        }

        Tenant tenantBeta = new Tenant("tenant-beta", "Empresa Beta Orion", "20987654321");
        tenantRepository.save(tenantBeta);

        Role driverRole = roleRepository.findByName("DRIVER")
                .orElseThrow(() -> new IllegalStateException("Rol DRIVER no encontrado"));
        Role managerRole = roleRepository.findByName("FLEET_MANAGER")
                .orElseThrow(() -> new IllegalStateException("Rol FLEET_MANAGER no encontrado"));

        User conductorBeta = new User("driver-beta", tenantBeta,
                "conductor2@empresa.com",
                passwordEncoder.encode("123456"),
                "Beta", "Conductor");
        conductorBeta.getRoles().add(driverRole);
        userRepository.save(conductorBeta);

        User gestorBeta = new User("manager-beta", tenantBeta,
                "gestor2@empresa.com",
                passwordEncoder.encode("123456"),
                "Beta", "Gestor");
        gestorBeta.getRoles().add(managerRole);
        userRepository.save(gestorBeta);

        log.info("[DataSeeder] Datos beta creados: tenant-beta, conductor2@empresa.com, gestor2@empresa.com");
    }

    private Permission getOrCreatePermission(String resource, AccessLevel level) {
        return permissionRepository.findByResourceAndAccessLevel(resource, level)
                .orElseGet(() -> permissionRepository.save(new Permission(resource, level)));
    }

    private Role getOrCreateRole(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name, description)));
    }
}
