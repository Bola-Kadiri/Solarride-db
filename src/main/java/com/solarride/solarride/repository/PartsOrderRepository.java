package com.solarride.solarride.repository;

import com.solarride.solarride.domain.parts.PartsOrder;
import com.solarride.solarride.domain.parts.PartsOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartsOrderRepository extends JpaRepository<PartsOrder, UUID> {

    Optional<PartsOrder> findByJobIdAndDeletedAtIsNull(UUID jobId);

    List<PartsOrder> findByJobIdAndStatus(UUID jobId, PartsOrderStatus status);
}