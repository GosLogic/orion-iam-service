package com.goslogic.orion.iam.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.goslogic.orion.iam.domain.model.Role;
import com.goslogic.orion.iam.domain.model.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        @JsonProperty("id") String externalId,
        String email,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        @JsonProperty("tenant_id") String tenantId,
        boolean active,
        Set<String> roles
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getExternalId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getTenant().getExternalId(),
                u.isActive(),
                u.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
        );
    }
}
