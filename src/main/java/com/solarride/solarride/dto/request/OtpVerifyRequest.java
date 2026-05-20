package com.solarride.solarride.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OtpVerifyRequest(

        @NotBlank @Pattern(regexp = "\\+?[0-9]{10,15}")
        String phone,

        @NotBlank @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        String otp
) {}