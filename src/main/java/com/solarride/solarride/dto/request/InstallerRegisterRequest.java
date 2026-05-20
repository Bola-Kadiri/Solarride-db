package com.solarride.solarride.dto.request;

import jakarta.validation.constraints.*;

public record InstallerRegisterRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Invalid phone number")
        String phone,

        @NotBlank @Size(min = 2, max = 100)
        String firstName,

        @NotBlank @Size(min = 2, max = 100)
        String lastName,

        @NotBlank @Size(min = 8)
        String password,

        @NotBlank
        String companyName,

        String cacNumber,
        String taxId,

        @NotBlank
        String shopAddress,

        Double latitude,
        Double longitude,

        @AssertTrue(message = "You must accept the SLA terms")
        boolean slaAccepted
) {}