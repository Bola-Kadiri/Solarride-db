package com.solarride.solarride.domain.installer;

import com.solarride.solarride.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "installer_certifications")
@Getter
@Setter
public class Certification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installer_id", nullable = false)
    private Installer installer;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private CertificationType documentType;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean verified = false;
}