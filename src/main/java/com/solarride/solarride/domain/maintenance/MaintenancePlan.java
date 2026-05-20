package com.solarride.solarride.domain.maintenance;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.job.Job;
import com.solarride.solarride.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maintenance_plans")
@Getter
@Setter
public class MaintenancePlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenancePlanTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenancePlanStatus status = MaintenancePlanStatus.ACTIVE;

    @Column(name = "next_visit_at")
    private Instant nextVisitAt;

    @Column(name = "billing_anchor_date")
    private LocalDate billingAnchorDate;

    @Column(name = "flutterwave_subscription_id")
    private String flutterwaveSubscriptionId;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}