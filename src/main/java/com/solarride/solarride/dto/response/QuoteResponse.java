package com.solarride.solarride.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        UUID jobId,
        UUID installerId,
        String installerName,
        String installerCompany,
        String status,
        BigDecimal labourCost,
        BigDecimal estimatedPartsCost,
        BigDecimal totalCost,
        LocalDate proposedStartDate,
        LocalDate proposedEndDate,
        String notes,
        Instant slaDeadlineAt,
        Instant createdAt
) {}