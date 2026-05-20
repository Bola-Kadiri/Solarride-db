package com.solarride.solarride.dto.response;

import java.math.BigDecimal;

public record CostEstimateResponse(
        String solarSize,
        String powerRange,
        BigDecimal estimatedCostMin,
        BigDecimal estimatedCostMax
) {}