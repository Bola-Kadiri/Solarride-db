package com.solarride.solarride.service.geo;

import com.solarride.solarride.domain.installer.Installer;
import com.solarride.solarride.dto.response.InstallerResponse;
import com.solarride.solarride.repository.InstallerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoMatchingService {

    private static final double DEFAULT_RADIUS_KM = 50.0;
    private static final double EXPANDED_RADIUS_KM = 100.0;
    private static final int MIN_RESULTS = 3;
    private static final int MAX_RESULTS = 10;

    private final InstallerRepository installerRepository;

    /**
     * Find nearby active installers ranked by:
     * (1/distanceKm)*0.4 + rating*0.4 + availabilityScore*0.2
     * Expands radius from 50km to 100km if fewer than 3 results.
     */
    @Transactional(readOnly = true)
    public List<InstallerResponse> findNearbyInstallers(double lat, double lng, Double requestedRadiusKm) {
        double radiusKm = requestedRadiusKm != null ? requestedRadiusKm : DEFAULT_RADIUS_KM;
        double radiusMetres = radiusKm * 1000.0;

        List<Installer> results = installerRepository.findNearbyActiveInstallers(lat, lng, radiusMetres, MAX_RESULTS);

        if (results.size() < MIN_RESULTS && radiusKm <= DEFAULT_RADIUS_KM) {
            log.info("Expanding radius from {}km to {}km (only {} results found)",
                    radiusKm, EXPANDED_RADIUS_KM, results.size());
            results = installerRepository.findNearbyActiveInstallers(
                    lat, lng, EXPANDED_RADIUS_KM * 1000.0, MAX_RESULTS);
        }

        log.info("Geo-match returned {} installers for ({}, {})", results.size(), lat, lng);
        return results.stream().map(this::toResponse).toList();
    }

    private InstallerResponse toResponse(Installer installer) {
        var user = installer.getUser();
        return new InstallerResponse(
                installer.getId(),
                user.getId(),
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                installer.getCompanyName(),
                installer.getShopAddress(),
                installer.getBadge().name(),
                user.getStatus().name(),
                installer.getAverageRating(),
                installer.getCompletedJobsCount(),
                installer.isAvailable());
    }
}