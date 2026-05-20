package com.solarride.solarride.service.job;

import com.solarride.solarride.domain.job.SolarSize;
import com.solarride.solarride.dto.response.CostEstimateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Indicative Lagos market rates (May 2026). These are estimates only; the
 * installer's formal quote supersedes them.
 */
@Slf4j
@Service
public class CostEstimationService {

    private static final BigDecimal STARTER_MIN  = new BigDecimal("350000");
    private static final BigDecimal STARTER_MAX  = new BigDecimal("700000");
    private static final BigDecimal STANDARD_MIN = new BigDecimal("900000");
    private static final BigDecimal STANDARD_MAX = new BigDecimal("1800000");
    private static final BigDecimal PREMIUM_MIN  = new BigDecimal("2000000");
    private static final BigDecimal PREMIUM_MAX  = new BigDecimal("4500000");
    private static final BigDecimal COMMERCIAL_MIN = new BigDecimal("5000000");
    private static final BigDecimal COMMERCIAL_MAX = new BigDecimal("15000000");

    public CostEstimateResponse estimate(SolarSize solarSize, double latitude, double longitude) {
        log.info("Cost estimate requested for {} at ({}, {})", solarSize, latitude, longitude);
        BigDecimal min = minFor(solarSize);
        BigDecimal max = maxFor(solarSize);
        return new CostEstimateResponse(solarSize.name(), solarSize.getPowerRange(), min, max);
    }

    public BigDecimal minFor(SolarSize size) {
        return switch (size) {
            case STARTER    -> STARTER_MIN;
            case STANDARD   -> STANDARD_MIN;
            case PREMIUM    -> PREMIUM_MIN;
            case COMMERCIAL -> COMMERCIAL_MIN;
        };
    }

    public BigDecimal maxFor(SolarSize size) {
        return switch (size) {
            case STARTER    -> STARTER_MAX;
            case STANDARD   -> STANDARD_MAX;
            case PREMIUM    -> PREMIUM_MAX;
            case COMMERCIAL -> COMMERCIAL_MAX;
        };
    }
}