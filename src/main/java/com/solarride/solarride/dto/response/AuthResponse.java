package com.solarride.solarride.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String role
) {}