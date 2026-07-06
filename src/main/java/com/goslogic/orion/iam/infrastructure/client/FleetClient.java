package com.goslogic.orion.iam.infrastructure.client;

import com.goslogic.orion.iam.infrastructure.config.FleetProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Cliente HTTP interno hacia orion-fleet-service (seam D16).
 * Usado en login DRIVER para resolver driver_id y vehicle_id reales.
 */
@Component
public class FleetClient {

    private static final Logger log = LoggerFactory.getLogger(FleetClient.class);

    private final RestClient restClient;

    public FleetClient(FleetProperties fleetProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(fleetProperties.getBaseUrl())
                .build();
    }

    /**
     * Resuelve driver external id y vehículo asignado desde Fleet.
     * Retorna empty si Fleet no responde o no hay datos (fallback D5 en AuthApplicationService).
     */
    public Optional<DriverFleetIdentity> resolveDriverIdentity(String userExternalId, String tenantExternalId) {
        try {
            FleetDriverDto driver = restClient.get()
                    .uri("/v1/fleet/drivers/by-user?user_external_id={userId}", userExternalId)
                    .header("X-Tenant-Id", tenantExternalId)
                    .retrieve()
                    .body(FleetDriverDto.class);

            if (driver == null || driver.externalId() == null) {
                return Optional.empty();
            }

            String vehicleExternalId = null;
            try {
                FleetVehicleDto vehicle = restClient.get()
                        .uri("/v1/fleet/vehicles/by-driver?driver_external_id={driverId}", driver.externalId())
                        .header("X-Tenant-Id", tenantExternalId)
                        .retrieve()
                        .body(FleetVehicleDto.class);
                if (vehicle != null) {
                    vehicleExternalId = vehicle.externalId();
                }
            } catch (RestClientException e) {
                log.warn("Fleet vehicle lookup failed for driver {}: {}", driver.externalId(), e.getMessage());
            }

            return Optional.of(new DriverFleetIdentity(driver.externalId(), vehicleExternalId));
        } catch (RestClientException e) {
            log.warn("Fleet driver lookup failed for user {}: {}", userExternalId, e.getMessage());
            return Optional.empty();
        }
    }
}
