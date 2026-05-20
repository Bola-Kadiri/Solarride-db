package com.solarride.solarride.domain.parts;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.job.Job;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bills_of_materials")
@Getter
@Setter
public class BillOfMaterials extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;

    @Column(name = "broadcast_deadline_at")
    private Instant broadcastDeadlineAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BomStatus status = BomStatus.PENDING;

    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomItem> items = new ArrayList<>();
}