package com.solarride.solarride.domain.supplier;

import com.solarride.solarride.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "supplier_documents")
@Getter
@Setter
public class SupplierDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "original_filename")
    private String originalFilename;
}