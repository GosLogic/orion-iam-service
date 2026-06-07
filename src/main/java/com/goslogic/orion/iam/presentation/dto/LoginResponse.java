package com.goslogic.orion.iam.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Respuesta de login. Compatible con el contrato de la app móvil (movil.md §6.1):
 *   driver_id, tenant_id, access_token, expires_at
 * El campo user_id se incluye para el login genérico web (no se envía en driver login si es null).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        @JsonProperty("driver_id")    String driverId,
        @JsonProperty("user_id")      String userId,
        @JsonProperty("tenant_id")    String tenantId,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_at")   String expiresAt,
        String email
) {
    public static LoginResponse forDriver(String driverId, String tenantId,
                                          String accessToken, String expiresAt, String email) {
        return new LoginResponse(driverId, null, tenantId, accessToken, expiresAt, email);
    }

    public static LoginResponse forWeb(String userId, String tenantId,
                                       String accessToken, String expiresAt, String email) {
        return new LoginResponse(null, userId, tenantId, accessToken, expiresAt, email);
    }
}
