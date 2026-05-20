package com.solarride.solarride.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminRejectRequest(
        @NotBlank
        String reason
) {}