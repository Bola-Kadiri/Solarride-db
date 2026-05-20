package com.solarride.solarride.repository;

import com.solarride.solarride.domain.supplier.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /**
     * Find active suppliers whose coverage area intersects the given job location point.
     */
    @Query(value = """
            SELECT s.*
            FROM suppliers s
            JOIN users u ON u.id = s.user_id
            WHERE u.status = 'ACTIVE'
              AND s.deleted_at IS NULL
              AND ST_Intersects(
                    s.coverage_area,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)
                  )
            """, nativeQuery = true)
    List<Supplier> findActiveSuppliersAtLocation(
            @Param("lat") double lat,
            @Param("lng") double lng);
}