package com.solarride.solarride.domain.installer;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "installers")
@Getter
@Setter
public class Installer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "cac_number")
    private String cacNumber;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "shop_address", columnDefinition = "TEXT")
    private String shopAddress;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    @Column(name = "service_radius_km", nullable = false)
    private int serviceRadiusKm = 50;

    @Column(name = "service_area", columnDefinition = "geometry(Geometry, 4326)")
    private Geometry serviceArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallerBadge badge = InstallerBadge.NEW_INSTALLER;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "completed_jobs_count", nullable = false)
    private int completedJobsCount = 0;

    @Column(name = "availability_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal availabilityScore = BigDecimal.ONE;

    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    @Column(name = "sla_accepted", nullable = false)
    private boolean slaAccepted = false;

    @Column(name = "sla_accepted_at")
    private Instant slaAcceptedAt;

    @Column(name = "flutterwave_subaccount_id")
    private String flutterwaveSubaccountId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "installer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Certification> certifications = new ArrayList<>();
}