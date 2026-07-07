package com.goslogic.orion.iam.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
        @NotNull Boolean active
) {}
