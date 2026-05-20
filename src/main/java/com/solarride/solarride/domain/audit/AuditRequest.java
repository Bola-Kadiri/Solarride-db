package com.solarride.solarride.domain.audit;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.job.Job;
import com.solarride.solarride.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_requests")
@Getter
@Setter
public class AuditRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id")
    private User auditor;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false)
    private AuditType auditType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status = AuditStatus.PENDING;

    @Column(precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(name = "site_visit_at")
    private Instant siteVisitAt;

    @Column(name = "report_deadline_at")
    private Instant reportDeadlineAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}