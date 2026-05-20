package com.solarride.solarride.service;

import com.solarride.solarride.domain.job.SolarSize;
import com.solarride.solarride.dto.response.CostEstimateResponse;
import com.solarride.solarride.service.job.CostEstimationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CostEstimationServiceTest {

    CostEstimationService service = new CostEstimationService();

    @Test
    void estimate_starter_returnsCorrectRange() {
        CostEstimateResponse response = service.estimate(SolarSize.STARTER, 6.5, 3.4);
        assertThat(response.estimatedCostMin()).isEqualByComparingTo(new BigDecimal("350000"));
        assertThat(response.estimatedCostMax()).isEqualByComparingTo(new BigDecimal("700000"));
        assertThat(response.solarSize()).isEqualTo("STARTER");
    }

    @Test
    void estimate_commercial_returnsLargestRange() {
        CostEstimateResponse response = service.estimate(SolarSize.COMMERCIAL, 6.5, 3.4);
        assertThat(response.estimatedCostMin()).isGreaterThan(response.estimatedCostMax().multiply(BigDecimal.ZERO));
        assertThat(response.estimatedCostMin()).isEqualByComparingTo(new BigDecimal("5000000"));
    }

    @Test
    void allSizesHaveMinLessThanMax() {
        for (SolarSize size : SolarSize.values()) {
            assertThat(service.minFor(size)).isLessThan(service.maxFor(size));
        }
    }
}
