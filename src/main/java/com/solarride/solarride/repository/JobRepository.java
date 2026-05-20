package com.solarride.solarride.repository;

import com.solarride.solarride.domain.job.Job;
import com.solarride.solarride.domain.job.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByCustomerIdAndDeletedAtIsNull(UUID customerId);

    List<Job> findByInstallerIdAndDeletedAtIsNull(UUID installerId);

    Optional<Job> findByIdAndDeletedAtIsNull(UUID id);

    List<Job> findByStatusAndDeletedAtIsNull(JobStatus status);

    long countByCustomerIdAndStatus(UUID customerId, JobStatus status);
}