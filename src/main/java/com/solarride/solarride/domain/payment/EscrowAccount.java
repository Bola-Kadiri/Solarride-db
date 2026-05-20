package com.solarride.solarride.domain.payment;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.job.Job;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "escrow_accounts")
@Getter
@Setter
public class EscrowAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus status = EscrowStatus.OPEN;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}