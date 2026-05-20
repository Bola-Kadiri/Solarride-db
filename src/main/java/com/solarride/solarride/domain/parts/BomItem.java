package com.solarride.solarride.domain.parts;

import com.solarride.solarride.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "bom_items")
@Getter
@Setter
public class BomItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bom_id", nullable = false)
    private BillOfMaterials bom;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    private String category;

    @Column(nullable = false)
    private int quantity = 1;

    private String unit;

    @Column(columnDefinition = "TEXT")
    private String specification;
}