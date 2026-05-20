package com.solarride.solarride.controller;

import com.solarride.solarride.dto.request.SupplierRegisterRequest;
import com.solarride.solarride.dto.response.SupplierResponse;
import com.solarride.solarride.security.SolarRideUserDetails;
import com.solarride.solarride.service.onboarding.SupplierOnboardingService;
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

@RestController
@RequestMapping("/api/v1/supplier")
@RequiredArgsConstructor
@Tag(name = "Suppliers")
public class SupplierController {

    private final SupplierOnboardingService supplierOnboardingService;

    @PostMapping("/register")
    @Operation(summary = "Register as a supplier")
    public ResponseEntity<SupplierResponse> register(@Valid @RequestBody SupplierRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierOnboardingService.register(request));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload supplier verification document")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            @AuthenticationPrincipal SolarRideUserDetails userDetails) {
        SupplierResponse profile = supplierOnboardingService.getProfile(userDetails.getUserId());
        return ResponseEntity.ok(
                supplierOnboardingService.uploadDocument(profile.supplierId(), file, documentType));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get own supplier profile")
    @PreAuthorize("hasRole('SUPPLIER')")
    public ResponseEntity<SupplierResponse> getProfile(
            @AuthenticationPrincipal SolarRideUserDetails userDetails) {
        return ResponseEntity.ok(supplierOnboardingService.getProfile(userDetails.getUserId()));
    }
}