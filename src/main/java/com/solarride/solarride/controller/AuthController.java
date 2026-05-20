package com.solarride.solarride.controller;

import com.solarride.solarride.dto.request.*;
import com.solarride.solarride.dto.response.AuthResponse;
import com.solarride.solarride.dto.response.OtpResponse;
import com.solarride.solarride.dto.response.UserResponse;
import com.solarride.solarride.security.SolarRideUserDetails;
import com.solarride.solarride.service.auth.AuthService;
import com.solarride.solarride.service.onboarding.CustomerOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final CustomerOnboardingService customerOnboardingService;

    @PostMapping("/customer/register")
    @Operation(summary = "Register a new customer")
    public ResponseEntity<UserResponse> registerCustomer(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerOnboardingService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Initiate login — sends OTP to registered phone")
    public ResponseEntity<OtpResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.initiateLogin(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Complete login with OTP — returns JWT tokens")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.completeLogin(request));
    }

    @PostMapping("/customer/verify-phone")
    @Operation(summary = "Verify phone number after registration")
    public ResponseEntity<UserResponse> verifyPhone(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(customerOnboardingService.verifyPhone(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshTokens(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — invalidates refresh token")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal SolarRideUserDetails userDetails) {
        authService.logout(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}