package com.solarride.solarride.repository;

import com.solarride.solarride.domain.audit.AuditRequest;
import com.solarride.solarride.domain.audit.AuditStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditRequestRepository extends JpaRepository<AuditRequest, UUID> {

    Optional<AuditRequest> findByIdAndDeletedAtIsNull(UUID id);

    List<AuditRequest> findByAuditorIdAndStatus(UUID auditorId, AuditStatus status);

    List<AuditRequest> findByStatus(AuditStatus status);
}