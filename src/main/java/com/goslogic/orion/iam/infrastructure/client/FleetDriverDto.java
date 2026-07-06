package com.goslogic.orion.iam.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FleetDriverDto(
        @JsonProperty("id") String externalId,
        @JsonProperty("tenant_id") String tenantExternalId
) {}
