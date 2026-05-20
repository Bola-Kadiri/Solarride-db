package com.solarride.solarride.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitQuoteRequest(
        @NotNull @DecimalMin("0.01") BigDecimal labourCost,
        @NotNull @DecimalMin("0.01") BigDecimal estimatedPartsCost,
        @NotNull LocalDate proposedStartDate,
        @NotNull LocalDate proposedEndDate,
        String notes
) {}