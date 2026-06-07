package com.goslogic.orion.iam.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 11) String ruc,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 6) String adminPassword,
        String adminFirstName,
        String adminLastName
) {}
