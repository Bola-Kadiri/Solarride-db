package com.solarride.solarride.controller;

import com.solarride.solarride.domain.installer.CertificationType;
import com.solarride.solarride.dto.request.InstallerRegisterRequest;
import com.solarride.solarride.dto.response.InstallerResponse;
import com.solarride.solarride.security.SolarRideUserDetails;
import com.solarride.solarride.service.geo.GeoMatchingService;
import com.solarride.solarride.service.onboarding.InstallerOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Installers")
public class InstallerController {

    private final InstallerOnboardingService installerOnboardingService;
    private final GeoMatchingService geoMatchingService;

    @PostMapping("/api/v1/installer/register")
    @Operation(summary = "Register as an installer")
    public ResponseEntity<InstallerResponse> register(@Valid @RequestBody InstallerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(installerOnboardingService.register(request));
    }

    @PostMapping(value = "/api/v1/installer/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload verification document")
    @PreAuthorize("hasRole('INSTALLER')")
    public ResponseEntity<InstallerResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") CertificationType documentType,
            @AuthenticationPrincipal SolarRideUserDetails userDetails) {
        InstallerResponse resp = installerOnboardingService.getProfile(userDetails.getUserId());
        return ResponseEntity.ok(
                installerOnboardingService.uploadDocument(resp.installerId(), file, documentType));
    }

    @GetMapping("/api/v1/installer/profile")
    @Operation(summary = "Get own installer profile")
    @PreAuthorize("hasRole('INSTALLER')")
    public ResponseEntity<InstallerResponse> getProfile(
            @AuthenticationPrincipal SolarRideUserDetails userDetails) {
        return ResponseEntity.ok(installerOnboardingService.getProfile(userDetails.getUserId()));
    }

    @PutMapping("/api/v1/installer/availability")
    @Operation(summary = "Toggle availability status")
    @PreAuthorize("hasRole('INSTALLER')")
    public ResponseEntity<InstallerResponse> toggleAvailability(
            @AuthenticationPrincipal SolarRideUserDetails userDetails) {
        return ResponseEntity.ok(installerOnboardingService.toggleAvailability(userDetails.getUserId()));
    }

    @GetMapping("/api/v1/installers/nearby")
    @Operation(summary = "Find nearby active installers by geo coordinates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InstallerResponse>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radiusKm) {
        return ResponseEntity.ok(geoMatchingService.findNearbyInstallers(lat, lng, radiusKm));
    }

    @GetMapping("/api/v1/installers/{installerId}")
    @Operation(summary = "Get installer profile by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InstallerResponse> getById(@PathVariable UUID installerId) {
        var installer = installerOnboardingService.getProfileByInstallerId(installerId);
        return ResponseEntity.ok(installer);
    }
}