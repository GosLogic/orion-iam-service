package com.goslogic.orion.iam.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.goslogic.orion.iam.domain.model.Tenant;

public record TenantResponse(
        @JsonProperty("id") String externalId,
        String name,
        String ruc,
        String status
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.getExternalId(), t.getName(), t.getRuc(),
                t.getStatus().name());
    }
}
