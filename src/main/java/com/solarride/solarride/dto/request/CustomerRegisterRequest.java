package com.solarride.solarride.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRegisterRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Invalid phone number")
        String phone,

        @NotBlank @Size(min = 2, max = 100)
        String firstName,

        @NotBlank @Size(min = 2, max = 100)
        String lastName,

        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {}