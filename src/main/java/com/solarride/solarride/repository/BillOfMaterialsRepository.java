package com.solarride.solarride.repository;

import com.solarride.solarride.domain.parts.BillOfMaterials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillOfMaterialsRepository extends JpaRepository<BillOfMaterials, UUID> {

    Optional<BillOfMaterials> findByJobId(UUID jobId);
}