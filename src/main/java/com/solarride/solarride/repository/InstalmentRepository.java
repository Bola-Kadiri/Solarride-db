package com.solarride.solarride.repository;

import com.solarride.solarride.domain.payment.Instalment;
import com.solarride.solarride.domain.payment.InstalmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstalmentRepository extends JpaRepository<Instalment, UUID> {

    List<Instalment> findByJobIdOrderByInstalmentNumber(UUID jobId);

    Optional<Instalment> findByJobIdAndInstalmentNumber(UUID jobId, int instalmentNumber);

    List<Instalment> findByJobIdAndStatus(UUID jobId, InstalmentStatus status);
}