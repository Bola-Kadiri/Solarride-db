package com.solarride.solarride.domain.audit;

import com.solarride.solarride.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_reports")
@Getter
@Setter
public class AuditReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_request_id", nullable = false, unique = true)
    private AuditRequest auditRequest;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "deficiencies_found", nullable = false)
    private boolean deficienciesFound = false;

    @Column(name = "submitted_at")
    private Instant submittedAt;
}