package com.solarride.solarride.domain.job;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.installer.Installer;
import com.solarride.solarride.domain.payment.PaymentPlan;
import com.solarride.solarride.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
public class Job extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installer_id")
    private Installer installer;

    @Enumerated(EnumType.STRING)
    @Column(name = "solar_size", nullable = false)
    private SolarSize solarSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_plan")
    private PaymentPlan paymentPlan;

    @Column(name = "property_type")
    private String propertyType;

    @Column(name = "property_location", columnDefinition = "geometry(Point, 4326)")
    private Point propertyLocation;

    @Column(name = "property_address", columnDefinition = "TEXT")
    private String propertyAddress;

    @Column(name = "preferred_start_date")
    private LocalDate preferredStartDate;

    @Column(name = "estimated_cost_min", precision = 19, scale = 4)
    private BigDecimal estimatedCostMin;

    @Column(name = "estimated_cost_max", precision = 19, scale = 4)
    private BigDecimal estimatedCostMax;

    @Column(name = "job_value", precision = 19, scale = 4)
    private BigDecimal jobValue;

    @Column(name = "parts_order_value", precision = 19, scale = 4)
    private BigDecimal partsOrderValue;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}