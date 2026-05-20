package com.solarride.solarride.domain.maintenance;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.installer.Installer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "maintenance_visits")
@Getter
@Setter
public class MaintenanceVisit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_plan_id", nullable = false)
    private MaintenancePlan maintenancePlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installer_id", nullable = false)
    private Installer installer;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitStatus status = VisitStatus.SCHEDULED;

    @Column(name = "report_s3_key")
    private String reportS3Key;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(name = "requires_remediation", nullable = false)
    private boolean requiresRemediation = false;
}