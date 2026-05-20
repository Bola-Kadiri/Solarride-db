package com.solarride.solarride.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InstallerResponse(
        UUID installerId,
        UUID userId,
        String email,
        String fullName,
        String companyName,
        String shopAddress,
        String badge,
        String status,
        BigDecimal averageRating,
        int completedJobsCount,
        boolean available
) {}