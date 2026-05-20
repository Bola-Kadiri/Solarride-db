package com.solarride.solarride.controller;

import com.solarride.solarride.dto.request.AdminRejectRequest;
import com.solarride.solarride.dto.response.InstallerResponse;
import com.solarride.solarride.dto.response.SupplierResponse;
import com.solarride.solarride.service.onboarding.InstallerOnboardingService;
import com.solarride.solarride.service.onboarding.SupplierOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final InstallerOnboardingService installerOnboardingService;
    private final SupplierOnboardingService supplierOnboardingService;

    @PostMapping("/installers/{id}/approve")
    @Operation(summary = "Approve installer application")
    public ResponseEntity<InstallerResponse> approveInstaller(@PathVariable UUID id) {
        return ResponseEntity.ok(installerOnboardingService.approve(id));
    }

    @PostMapping("/installers/{id}/reject")
    @Operation(summary = "Reject installer application with reason")
    public ResponseEntity<InstallerResponse> rejectInstaller(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRejectRequest request) {
        return ResponseEntity.ok(installerOnboardingService.reject(id, request.reason()));
    }

    @PostMapping("/suppliers/{id}/approve")
    @Operation(summary = "Approve supplier application")
    public ResponseEntity<SupplierResponse> approveSupplier(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierOnboardingService.approve(id));
    }

    @PostMapping("/suppliers/{id}/reject")
    @Operation(summary = "Reject supplier application with reason")
    public ResponseEntity<SupplierResponse> rejectSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody AdminRejectRequest request) {
        return ResponseEntity.ok(supplierOnboardingService.reject(id, request.reason()));
    }
}