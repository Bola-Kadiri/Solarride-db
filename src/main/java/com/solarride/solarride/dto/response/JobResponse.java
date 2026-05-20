package com.solarride.solarride.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobResponse(
        UUID id,
        UUID customerId,
        UUID installerId,
        String solarSize,
        String status,
        String paymentPlan,
        String propertyType,
        String propertyAddress,
        Double latitude,
        Double longitude,
        LocalDate preferredStartDate,
        BigDecimal estimatedCostMin,
        BigDecimal estimatedCostMax,
        BigDecimal jobValue,
        Instant createdAt
) {}