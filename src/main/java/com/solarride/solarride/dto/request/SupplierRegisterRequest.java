package com.solarride.solarride.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record SupplierRegisterRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Invalid phone number")
        String phone,

        @NotBlank @Size(min = 2, max = 100)
        String repFirstName,

        @NotBlank @Size(min = 2, max = 100)
        String repLastName,

        @NotBlank @Size(min = 8)
        String password,

        @NotBlank
        String companyName,

        String registrationNumber,
        String taxId,

        @Min(1)
        Integer deliveryLeadTimeDays,

        List<String> productCategories,

        @AssertTrue(message = "You must accept the SLA terms")
        boolean slaAccepted
) {}