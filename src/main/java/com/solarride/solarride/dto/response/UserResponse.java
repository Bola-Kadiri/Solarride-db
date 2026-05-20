package com.solarride.solarride.dto.response;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        String role,
        String status,
        boolean emailVerified,
        boolean phoneVerified,
        String homeAddress
) {}