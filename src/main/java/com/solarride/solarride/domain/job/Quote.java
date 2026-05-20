package com.solarride.solarride.domain.job;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.installer.Installer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "quotes")
@Getter
@Setter
public class Quote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installer_id", nullable = false)
    private Installer installer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status = QuoteStatus.REQUESTED;

    @Column(name = "labour_cost", precision = 19, scale = 4)
    private BigDecimal labourCost;

    @Column(name = "estimated_parts_cost", precision = 19, scale = 4)
    private BigDecimal estimatedPartsCost;

    @Column(name = "total_cost", precision = 19, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "proposed_start_date")
    private LocalDate proposedStartDate;

    @Column(name = "proposed_end_date")
    private LocalDate proposedEndDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sla_deadline_at")
    private Instant slaDeadlineAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}