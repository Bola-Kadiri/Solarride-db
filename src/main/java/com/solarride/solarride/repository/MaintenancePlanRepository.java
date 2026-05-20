package com.solarride.solarride.repository;

import com.solarride.solarride.domain.maintenance.MaintenancePlan;
import com.solarride.solarride.domain.maintenance.MaintenancePlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, UUID> {

    List<MaintenancePlan> findByCustomerIdAndDeletedAtIsNull(UUID customerId);

    List<MaintenancePlan> findByStatus(MaintenancePlanStatus status);
}