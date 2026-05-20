package com.solarride.solarride.repository;

import com.solarride.solarride.domain.installer.Installer;
import com.solarride.solarride.domain.user.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstallerRepository extends JpaRepository<Installer, UUID> {

    Optional<Installer> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /**
     * Geo-matching query: find active installers within radiusMetres of the given point,
     * ranked by the platform formula: (1/distance)*0.4 + rating*0.4 + availabilityScore*0.2
     */
    @Query(value = """
            SELECT i.*,
                   ST_Distance(i.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS dist_m
            FROM installers i
            JOIN users u ON u.id = i.user_id
            WHERE u.status = 'ACTIVE'
              AND i.deleted_at IS NULL
              AND i.is_available = TRUE
              AND ST_DWithin(
                    i.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                    :radiusMetres
                  )
            ORDER BY
                (1.0 / NULLIF(ST_Distance(i.location::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) / 1000.0, 0)) * 0.4
                + i.average_rating * 0.4
                + i.availability_score * 0.2 DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Installer> findNearbyActiveInstallers(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMetres") double radiusMetres,
            @Param("limit") int limit);

    List<Installer> findAllByDeletedAtIsNullAndUser_Status(UserStatus status);
}