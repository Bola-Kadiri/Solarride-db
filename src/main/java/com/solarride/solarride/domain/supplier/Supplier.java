package com.solarride.solarride.domain.supplier;

import com.solarride.solarride.domain.BaseEntity;
import com.solarride.solarride.domain.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
public class Supplier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "rep_name")
    private String repName;

    @Column(name = "coverage_area", columnDefinition = "geometry(Geometry, 4326)")
    private Geometry coverageArea;

    @Column(name = "delivery_lead_time_days")
    private Integer deliveryLeadTimeDays;

    @Column(name = "sla_accepted", nullable = false)
    private boolean slaAccepted = false;

    @Column(name = "sla_accepted_at")
    private Instant slaAcceptedAt;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "flutterwave_subaccount_id")
    private String flutterwaveSubaccountId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCategory> productCategories = new ArrayList<>();

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierDocument> documents = new ArrayList<>();
}