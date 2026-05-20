package com.solarride.solarride.dto.request;

import com.solarride.solarride.domain.job.SolarSize;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateJobRequest(
        @NotNull SolarSize solarSize,
        @NotBlank String propertyType,
        @NotBlank String propertyAddress,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        LocalDate preferredStartDate
) {}