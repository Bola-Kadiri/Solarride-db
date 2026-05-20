# SolarRide — CLAUDE.md
> Claude Code project instructions for the SolarRide platform.
> Read this file before generating, editing, or reviewing any code in this repository.

---

## Project overview

SolarRide is an Uber-for-Solar marketplace platform built with **Java 21 + Spring Boot 3.x**.
It connects property owners (customers) with certified solar installers and verified parts
suppliers across Nigeria, starting with Lagos.

The platform handles geo-matched job booking, escrow-protected milestone payments,
parts procurement, post-installation maintenance, solar audits, and a full admin analytics
dashboard. All financial transactions flow through an escrow engine before disbursement.

---

## Complete platform flow (all 8 phases)

> This section is the authoritative reference for how every user action maps to
> a system event, a service method, and a domain state change.
> Claude Code must respect this flow when generating any code across all modules.

---

### Phase 1 — Platform onboarding (signup & signin)

Three user types onboard in parallel. All three must complete their respective
signup before any job transaction can begin.

#### Installer signup flow

```
1.  Installer visits partner portal (web or mobile)
2.  Selects "Register as installer"
3.  Submits personal details → name, email, phone, password
4.  Submits business details → company name, CAC number, tax ID
5.  Uploads certifications → solar licence, liability insurance, govt-issued ID
    [stored in S3 via S3StorageService; references saved to Certification entity]
6.  Sets service area → pins location on map (PostGIS Point), sets radius (km)
7.  Accepts SLA & commission terms → e-sign stored as boolean + timestamp on Installer
8.  Account status → PENDING_VERIFICATION
    [AdminNotificationEvent fired → admin receives push + SMS]
9.  Admin reviews documents within 5 business days
    → APPROVED: status → ACTIVE, badge → NEW_INSTALLER, profile visible on map
    → REJECTED: email + SMS sent with reason; installer may resubmit documents
10. Approved installer completes online orientation module
11. Profile goes live on geo-matching map
```

**Signin (returning installer):**
```
email + password → OTP via Termii SMS → JWT issued (role: INSTALLER) → dashboard
```

#### Customer (property owner) signup flow

```
1.  Opens SolarRide app (iOS / Android / web)
2.  Selects "Sign up as customer" or continues with Google / Apple SSO
3.  Enters name, email, phone, password
4.  OTP phone verification via Termii (6-digit SMS code, 3 attempts, 60s cooldown)
5.  Email verification link sent via email service
6.  Pins property location on map → address + property type stored
7.  Accepts terms & privacy policy
8.  Adds payment method → card / bank transfer / mobile money via Flutterwave
9.  Account status → ACTIVE immediately (no admin vetting required)
10. Lands on customer home dashboard
11. Ready to post first job request
```

**Signin (returning customer):**
```
email + password → OTP or biometric → home dashboard (role: CUSTOMER)
```

#### Supplier signup flow

```
1.  Visits supplier portal (desktop-first web)
2.  Selects "Register as supplier"
3.  Enters company name, registration number, tax ID, rep name, email, phone, password
4.  Uploads business documents → CAC cert, tax clearance, director ID
    [stored in S3; references saved to Supplier entity]
5.  Declares product catalogue → categories, brands, minimum stock levels
6.  Sets delivery coverage area → states + LGAs + delivery lead time
    [stored as PostGIS Geometry polygon on Supplier]
7.  Accepts supplier SLA terms → 5% commission, quality policy, e-sign
8.  Account status → PENDING_VERIFICATION
9.  Admin verifies within 5 business days
    → APPROVED: status → ACTIVE, supplier dashboard accessible
    → REJECTED: email with reason, resubmit path available
10. Supplier dashboard live → ready to receive RFQs
```

**Signin (returning supplier):**
```
email + password → OTP verify → supplier dashboard (role: SUPPLIER)
```

#### Convergence point

Once all three user types are live on the platform, the job lifecycle begins.
All subsequent phases flow through a single shared job context.

---

### Phase 2 — Job request, solar sizing & installer matching

```
1.  Customer submits job request
    → JobService.createJob() called
    → Job entity created with status: DRAFT
    → Fields: propertyType, location (PostGIS Point), preferredStartDate

2.  Customer selects solar system size
    → Stored as SolarSize enum on Job entity
    → Size tiers:
        STARTER    → 1–2 kW   → basic home (lights, fans, TV, phone charging)
        STANDARD   → 3–5 kW   → family home (fridge, pump, small AC)
        PREMIUM    → 6–10 kW  → large home (multiple ACs, full load)
        COMMERCIAL → 11 kW+   → business / estate (3-phase systems)

3.  Platform estimates project cost
    → CostEstimationService.estimate(solarSize, location) called
    → Returns indicative price range based on size tier + Lagos market rates
    → Stored as estimatedCostMin and estimatedCostMax on Job

4.  Geo-matching engine runs
    → GeoMatchingService.findNearbyInstallers(jobLocation, radiusKm) called
    → PostGIS query: ST_DWithin + ST_Distance against installers table
    → Default radius: 50 km; expands to 100 km if < 3 results found
    → Ranking formula: (1 / distanceKm) * 0.4 + rating * 0.4 + availabilityScore * 0.2
    → Returns up to 10 ACTIVE installers

5.  Customer views installer profiles
    → Profile shows: certification badges, completed job count,
      average rating, average response time, indicative price range

6.  Customer requests quotes from 1–3 installers
    → QuoteService.requestQuote(jobId, installerId) called per installer
    → Quote entity created with status: REQUESTED
    → 24-hour SLA countdown starts → SlaMonitorJob tracks this
    → Installer notified via NotificationOrchestrator (FCM + SMS)

7.  Installer submits formal quote within 24 hours
    → Quote fields: labourCost, estimatedPartsCost, totalCost,
      proposedStartDate, proposedEndDate, notes
    → Quote status → SUBMITTED
    → Customer notified via NotificationOrchestrator

8.  Customer selects preferred installer
    → QuoteService.acceptQuote(jobId, quoteId) called
    → Accepted quote status → ACCEPTED; others → DECLINED
    → Job status → QUOTED
    → Proceed to Phase 3 (payment)
```

---

### Phase 3 — Payment selection & escrow

```
1.  Customer selects payment plan
    → PaymentPlanService.selectPlan(jobId, paymentPlan) called
    → Two options presented:

    ┌─────────────────────────────────────────────────────────────────┐
    │  OUTRIGHT                    │  THREE_INSTALMENT                │
    │  Full amount paid at booking │  +15% interest on total          │
    │  0% surcharge                │  Split into 3 equal payments     │
    │  Instant escrow              │                                  │
    │                              │  Instalment 1: at booking        │
    │                              │  Instalment 2: at mid-install    │
    │                              │  Instalment 3: at completion     │
    └─────────────────────────────────────────────────────────────────┘

2.  THREE_INSTALMENT interest calculation (always use BigDecimal):
    BigDecimal totalWithInterest = jobValue.multiply(new BigDecimal("1.15"));
    BigDecimal instalmentAmount  = totalWithInterest
        .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
    → 3 Instalment entities created with status: PENDING

3.  Payment initiated
    → PaymentController.initiatePayment() called
    → FlutterwaveClient.initializeTransaction() called
    → Returns Flutterwave hosted payment page URL to customer

4.  Customer completes payment on Flutterwave
    → Flutterwave fires webhook to POST /api/v1/payments/webhook
    → FlutterwaveWebhookController verifies HMAC-SHA256 signature
    → Idempotency check: transactionReference must be unique
    → EscrowService.fundEscrow(jobId, amount) called [@Transactional]
    → EscrowAccount credited, Transaction record created
    → Job status → CONFIRMED
    → Installer notified: "Job confirmed, parts being sourced"
    → Customer notified: "Payment received, your installer is confirmed"

    For THREE_INSTALMENT: Instalment 1 status → PAID
    InstalmentReminderJob schedules reminder for Instalment 2 trigger
```

---

### Phase 4 — Parts procurement (M2)

```
1.  BOM auto-generated on job CONFIRMED
    → BomGenerationService.generateBom(jobId) called
    → BillOfMaterials entity created from SolarSize + installer quote specs
    → BOM items include: panels (qty, watt), inverter (kVA), batteries,
      mounting hardware, wiring, isolators, breakers

2.  BOM broadcast to parts pool
    → PartsOrderService.broadcastBom(bomId) called
    → PostGIS query: suppliers whose coverageArea intersects jobLocation
    → Only ACTIVE suppliers with matching product categories receive broadcast
    → Suppliers notified via NotificationOrchestrator (FCM + SMS)
    → 6-hour response window starts

3.  Suppliers respond with quote
    → POST /api/v1/supplier/rfq/{bomId}/quote
    → Fields: unitPrices per item, totalPrice, availabilityConfirmed, deliveryDate

4.  Best offer selected
    → Selection criteria: lowest totalPrice WHERE deliveryDate ≤ jobStartDate - 2 days
    → Auto-selected or installer confirms
    → PartsOrder entity created with status: CONFIRMED
    → Supplier notified: "Your parts order has been confirmed"
    → Parts cost disbursed from escrow to supplier sub-account on delivery confirmation
      (not upfront — paid on installer receipt confirmation)

5.  Supplier dispatches parts
    → PartsOrder status → DISPATCHED
    → Tracking ID stored on PartsOrder
    → Installer notified with tracking details

6.  Installer confirms parts received
    → POST /api/v1/jobs/{jobId}/parts/confirm-receipt
    → PartsOrder status → DELIVERED
    → Job status → IN_PROGRESS (installation can begin)
    → Parts payment released from escrow to supplier: amount - 5% commission
      [@Transactional in EscrowService]
```

---

### Phase 5 — Installation execution

```
1.  Installer attends site
    → JobLifecycleService.markInProgress(jobId) called
    → Job status confirmed as IN_PROGRESS
    → Customer sees real-time status update via WebSocket

2.  THREE_INSTALMENT ONLY: Instalment 2 triggered at mid-installation milestone
    → Installer calls POST /api/v1/jobs/{jobId}/milestones/mounting-complete
    → Job milestone: MOUNTING_COMPLETE stored
    → InstalmentService.triggerInstalment(jobId, 2) called
    → Customer receives FCM push + SMS: "Instalment 2 now due"
    → Customer pays via app → EscrowService.fundEscrow() called
    → Instalment 2 status → PAID

3.  Installation stages (logged as JobMilestone records):
    → SITE_SURVEY_COMPLETE
    → MOUNTING_COMPLETE        ← triggers Instalment 2 for THREE_INSTALMENT
    → PANELS_FITTED
    → WIRING_COMPLETE
    → SYSTEM_TESTED
    → HANDOVER_COMPLETE

4.  Job marked complete
    → Installer calls POST /api/v1/jobs/{jobId}/complete
    → Uploads evidence: minimum 3 photos + signed customer handover sheet (S3)
    → Job status → COMPLETED
    → 48-hour customer review window opens
    → Customer notified: "Your installation is complete. Please verify."

    THREE_INSTALMENT ONLY: Instalment 3 triggered
    → InstalmentService.triggerInstalment(jobId, 3) called
    → Customer pays final instalment before escrow release proceeds
    → Instalment 3 status → PAID
```

---

### Phase 6 — Verification, dispute & payment release

```
1.  Customer review window (48 hours)
    → EscrowReleaseJob scheduled in Quartz: fires at completedAt + 48 hours
    → Customer has two options:

    PATH A — No dispute (happy path):
    → 48-hour timer expires with no dispute
    → EscrowReleaseJob fires
    → EscrowService.releaseEscrow(jobId) called [@Transactional]
    → In ONE atomic transaction:
        a. Supplier net payout = partsOrderValue * 0.95  (5% platform commission)
        b. Labour value = jobValue - partsOrderValue
        c. Commission rate determined by installer badge:
             TOP_INSTALLER → 10%, STANDARD/NEW → 12%
        d. Installer net payout = labourValue * (1 - commissionRate)
        e. Platform retains: supplier commission + installer commission
        f. Transaction records created for all three disbursements
        g. Flutterwave split payments fired via FlutterwaveClient
    → Job status → PAID
    → Installer notified: "Payment of ₦X released to your account"
    → Supplier notified: "Payment of ₦X released to your account"

    PATH B — Dispute raised:
    → Customer calls POST /api/v1/jobs/{jobId}/dispute within 48 hours
    → Dispute entity created with description + evidence uploads (S3)
    → EscrowReleaseJob Quartz trigger CANCELLED (funds remain frozen)
    → Job status → DISPUTED
    → Admin notified via FCM + SMS
    → Admin reviews within 72 hours:
        → FULL_RELEASE: escrow released to installer (Path A flow)
        → PARTIAL_REFUND: partial release to installer, remainder refunded to customer
        → FULL_REFUND: full refund to customer, installer penalised on rating

2.  Post-release
    → Customer prompted to submit rating (1–5 stars + written review)
    → ReviewService.submitReview(jobId, rating, comment) called
    → Installer's averageRating recalculated
    → Badge eligibility re-evaluated by nightly scheduled job
    → Job status → CLOSED
```

---

### Phase 7 — Post-installation services

```
1.  Platform prompts maintenance upsell
    → Triggered 24 hours after job status → CLOSED
    → Push notification + in-app prompt
    → Customer presented with three plan tiers:

    BASIC:    Annual visit    → visual inspection, cleaning, report         → 15% commission
    STANDARD: Bi-annual       → above + inverter check, firmware update     → 15% commission
    PREMIUM:  Quarterly       → above + thermographic imaging, battery health→ 15% commission
    EMERGENCY: On-demand      → fault diagnosis, same/next-day repair        → 12% commission

2.  Customer subscribes to plan
    → MaintenanceService.subscribePlan(jobId, tier) called
    → MaintenancePlan entity created, linked to original Job
    → Monthly subscription fee auto-charged via Flutterwave recurring charge
    → First visit scheduled automatically based on tier frequency

3.  Maintenance visit executed
    → Assigned to original installer (preferred) or nearest available
    → Installer uploads MaintenanceVisit report to S3
    → Customer receives digital report
    → Any remediation found → platform auto-creates new DRAFT Job (M1 loop)

4.  Audit requested (any time post-installation)
    → Customer selects audit type: PERFORMANCE / COMPLIANCE / PRE_PURCHASE / INSURANCE
    → AuditService.requestAudit(jobId, auditType) called
    → AuditRequest entity created
    → Nearest available AUDITOR role user assigned
    → Auditor visits site within agreed window
    → AuditReport PDF uploaded to S3 within 48 hours (SLA enforced)
    → 18% platform commission on audit fee
    → If deficiencies found → platform auto-prompts remediation job booking

5.  NOT NOW path
    → Customer skips upsell
    → MaintenanceReminderJob schedules re-prompt in 30 days
    → Stored as a Quartz trigger on the Job record
```

---

### Phase 8 — Platform analytics & admin (M7)

```
Every completed transaction feeds into AnalyticsService in real time.
Analytics queries run against a PostgreSQL read replica.
Heavy aggregations cached in Redis (TTL: 5 minutes).

Admin dashboard panels updated continuously:
→ Revenue KPIs: GTV, net revenue, commission by stream (install / parts / maintenance / audit)
→ Job pipeline: by status, by LGA, average completion time
→ Installer league table: rating, job count, dispute rate, revenue generated
→ Supplier performance: fill rate, on-time delivery %, defect rate
→ Customer metrics: new vs returning, NPS, maintenance attach rate, average job value
→ SLA compliance: quote response rate (24hr), audit delivery rate (48hr), payout release rate
→ Dispute log: open disputes, average resolution time, outcome distribution
→ Geographic heatmap: job density by Lagos LGA (PostGIS aggregation)

Insights drive platform optimisation:
→ Coverage gaps → admin recruits installers in underserved areas
→ Low attach rate → product team adjusts maintenance upsell copy
→ SLA breach spike → ops team investigates installer capacity
→ High dispute rate on specific installer → performance review triggered

Loop:
→ Customer books next job or service → return to Phase 2
→ Recurring maintenance visits → ongoing revenue without new acquisition cost
```

---

### Platform state machine summary

```
Job status transitions (enforced exclusively by JobLifecycleService):

DRAFT
  → QUOTED        (after at least one quote submitted by installer)
  → CANCELLED     (customer cancels before payment)

QUOTED
  → CONFIRMED     (after escrow payment received and verified)
  → CANCELLED     (quote expired or customer withdraws)

CONFIRMED
  → IN_PROGRESS   (after installer confirms parts received)

IN_PROGRESS
  → COMPLETED     (installer marks complete + uploads evidence)

COMPLETED
  → PAID          (escrow released — no dispute in 48hr window)
  → DISPUTED      (customer raises dispute within 48hr window)

DISPUTED
  → PAID          (admin resolves in installer's favour)
  → PARTIALLY_REFUNDED (admin resolves with partial refund)
  → REFUNDED      (admin resolves in customer's favour)

PAID / CLOSED     (terminal state — no further transitions)
```

---

### SLA reference table

| Commitment | Responsible party | SLA window | Breach consequence |
|---|---|---|---|
| Installer quote response | Installer | 24 hours | Job redistributed; installer flagged |
| Customer booking after quote | Customer | 48 hours | Quote expires; job re-broadcast |
| Supplier parts quote response | Supplier | 6 hours | Supplier skipped; next supplier tried |
| Parts delivery to installer | Supplier | As quoted, max 5 business days | Supplier rating deduction |
| Admin vetting (installer/supplier) | Admin | 5 business days | Applicant notified of delay |
| Dispute resolution | Admin | 72 hours | Auto-escalated to senior admin |
| Escrow release to installer | Platform | 24 hours after release trigger | Platform records delay |
| Audit report delivery | Auditor | 48 hours after site visit | Partial fee refund to customer |

---

## Technology stack

| Layer | Technology |
|---|---|
| Language | Java 21 (virtual threads via Project Loom enabled) |
| Framework | Spring Boot 3.3.x |
| API style | Spring MVC (REST) — synchronous; WebFlux only for WebSocket endpoints |
| ORM | Spring Data JPA + Hibernate 6 |
| Geospatial | Hibernate Spatial + JTS Core + PostGIS |
| Database | PostgreSQL 16 + PostGIS 3.4 |
| Cache | Redis 7 via Spring Data Redis (Lettuce client) |
| Background jobs | Quartz Scheduler |
| Migrations | Flyway |
| Security | Spring Security 6 + JJWT (JWT) |
| WebSocket | Spring WebSocket + STOMP |
| HTTP client | WebClient (Spring WebFlux) — for Flutterwave, Google Maps, Termii |
| Payments | Flutterwave API (primary), Paystack API (fallback) |
| SMS / OTP | Termii API |
| Push notifications | Firebase Admin SDK (FCM) |
| Maps | Google Maps Platform (Geocoding + Distance Matrix + Maps JavaScript) |
| File storage | AWS S3 via AWS SDK v2 |
| Docs | SpringDoc OpenAPI 3 (Swagger UI at /swagger-ui.html) |
| Monitoring | Spring Actuator + Sentry Spring Boot SDK |
| Testing | JUnit 5 + Mockito + Testcontainers + RestAssured |
| Build | Maven 3.9 |
| Containerisation | Docker + Docker Compose (local), Kubernetes/EKS (production) |

---

## Project structure

```
solarride/
├── CLAUDE.md                          ← This file
├── pom.xml
├── docker-compose.yml                 ← PostgreSQL+PostGIS, Redis (local dev)
├── .env.example                       ← All required environment variables
└── src/
    ├── main/
    │   ├── java/com/solarride/
    │   │   ├── SolarRideApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   ├── RedisConfig.java
    │   │   │   ├── WebSocketConfig.java
    │   │   │   ├── QuartzConfig.java
    │   │   │   ├── S3Config.java
    │   │   │   └── WebClientConfig.java
    │   │   ├── domain/                ← JPA entities
    │   │   │   ├── user/
    │   │   │   │   ├── User.java
    │   │   │   │   ├── Role.java      (enum: CUSTOMER, INSTALLER, SUPPLIER, AUDITOR, ADMIN)
    │   │   │   │   └── UserStatus.java (enum: PENDING, ACTIVE, SUSPENDED)
    │   │   │   ├── installer/
    │   │   │   │   ├── Installer.java
    │   │   │   │   ├── InstallerBadge.java (enum: NEW, STANDARD, TOP)
    │   │   │   │   └── Certification.java
    │   │   │   ├── supplier/
    │   │   │   │   ├── Supplier.java
    │   │   │   │   └── ProductCategory.java
    │   │   │   ├── job/
    │   │   │   │   ├── Job.java
    │   │   │   │   ├── JobStatus.java (enum: DRAFT, QUOTED, CONFIRMED, IN_PROGRESS, COMPLETED, DISPUTED, CANCELLED)
    │   │   │   │   ├── SolarSize.java (enum: STARTER, STANDARD, PREMIUM, COMMERCIAL)
    │   │   │   │   ├── Quote.java
    │   │   │   │   └── JobEvidence.java
    │   │   │   ├── parts/
    │   │   │   │   ├── BillOfMaterials.java
    │   │   │   │   ├── BomItem.java
    │   │   │   │   ├── PartsOrder.java
    │   │   │   │   └── PartsOrderStatus.java (enum: PENDING, CONFIRMED, DISPATCHED, DELIVERED)
    │   │   │   ├── payment/
    │   │   │   │   ├── EscrowAccount.java
    │   │   │   │   ├── Transaction.java
    │   │   │   │   ├── PaymentPlan.java (enum: OUTRIGHT, THREE_INSTALMENT)
    │   │   │   │   ├── Instalment.java
    │   │   │   │   └── InstalmentStatus.java (enum: PENDING, PAID, OVERDUE)
    │   │   │   ├── maintenance/
    │   │   │   │   ├── MaintenancePlan.java
    │   │   │   │   ├── MaintenancePlanTier.java (enum: BASIC, STANDARD, PREMIUM)
    │   │   │   │   └── MaintenanceVisit.java
    │   │   │   ├── audit/
    │   │   │   │   ├── AuditRequest.java
    │   │   │   │   ├── AuditType.java (enum: PERFORMANCE, COMPLIANCE, PRE_PURCHASE, INSURANCE)
    │   │   │   │   └── AuditReport.java
    │   │   │   └── review/
    │   │   │       └── Review.java
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   ├── InstallerRepository.java   ← PostGIS geo queries here
    │   │   │   ├── JobRepository.java
    │   │   │   ├── QuoteRepository.java
    │   │   │   ├── BillOfMaterialsRepository.java
    │   │   │   ├── PartsOrderRepository.java
    │   │   │   ├── EscrowAccountRepository.java
    │   │   │   ├── TransactionRepository.java
    │   │   │   ├── InstalmentRepository.java
    │   │   │   ├── MaintenancePlanRepository.java
    │   │   │   ├── AuditRequestRepository.java
    │   │   │   └── ReviewRepository.java
    │   │   ├── service/
    │   │   │   ├── auth/
    │   │   │   │   ├── AuthService.java
    │   │   │   │   ├── JwtService.java
    │   │   │   │   └── OtpService.java
    │   │   │   ├── onboarding/
    │   │   │   │   ├── CustomerOnboardingService.java
    │   │   │   │   ├── InstallerOnboardingService.java
    │   │   │   │   └── SupplierOnboardingService.java
    │   │   │   ├── geo/
    │   │   │   │   └── GeoMatchingService.java  ← PostGIS radius search + ranking
    │   │   │   ├── job/
    │   │   │   │   ├── JobService.java
    │   │   │   │   ├── QuoteService.java
    │   │   │   │   └── JobLifecycleService.java  ← State machine for job status transitions
    │   │   │   ├── parts/
    │   │   │   │   ├── BomGenerationService.java
    │   │   │   │   └── PartsOrderService.java
    │   │   │   ├── payment/
    │   │   │   │   ├── EscrowService.java        ← CRITICAL — all @Transactional
    │   │   │   │   ├── PaymentPlanService.java
    │   │   │   │   ├── InstalmentService.java
    │   │   │   │   ├── CommissionService.java
    │   │   │   │   └── PayoutService.java
    │   │   │   ├── maintenance/
    │   │   │   │   └── MaintenanceService.java
    │   │   │   ├── audit/
    │   │   │   │   └── AuditService.java
    │   │   │   ├── notification/
    │   │   │   │   ├── PushNotificationService.java  ← FCM
    │   │   │   │   ├── SmsService.java               ← Termii
    │   │   │   │   └── NotificationOrchestrator.java ← Always send both push + SMS
    │   │   │   ├── storage/
    │   │   │   │   └── S3StorageService.java
    │   │   │   └── analytics/
    │   │   │       └── AnalyticsService.java
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   ├── CustomerController.java
    │   │   │   ├── InstallerController.java
    │   │   │   ├── SupplierController.java
    │   │   │   ├── JobController.java
    │   │   │   ├── QuoteController.java
    │   │   │   ├── PartsController.java
    │   │   │   ├── PaymentController.java
    │   │   │   ├── MaintenanceController.java
    │   │   │   ├── AuditController.java
    │   │   │   ├── ReviewController.java
    │   │   │   ├── AdminController.java
    │   │   │   └── WebSocketController.java
    │   │   ├── dto/
    │   │   │   ├── request/              ← One per API operation
    │   │   │   └── response/             ← One per API operation
    │   │   ├── security/
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   ├── SolarRideUserDetails.java
    │   │   │   └── SolarRideUserDetailsService.java
    │   │   ├── scheduler/
    │   │   │   ├── EscrowReleaseJob.java      ← Auto-release after 48-hr dispute window
    │   │   │   ├── SlaMonitorJob.java         ← SLA breach detection + alerts
    │   │   │   ├── InstalmentReminderJob.java ← 3-instalment payment reminders
    │   │   │   └── MaintenanceSchedulerJob.java
    │   │   ├── event/
    │   │   │   ├── JobCompletedEvent.java
    │   │   │   ├── PaymentReceivedEvent.java
    │   │   │   ├── DisputeRaisedEvent.java
    │   │   │   └── EscrowReleasedEvent.java
    │   │   ├── external/
    │   │   │   ├── flutterwave/
    │   │   │   │   ├── FlutterwaveClient.java
    │   │   │   │   └── FlutterwaveWebhookController.java
    │   │   │   ├── googlemaps/
    │   │   │   │   └── GoogleMapsClient.java
    │   │   │   └── termii/
    │   │   │       └── TermiiClient.java
    │   │   └── exception/
    │   │       ├── GlobalExceptionHandler.java
    │   │       ├── InsufficientEscrowException.java
    │   │       ├── JobStateException.java
    │   │       ├── InstallerNotAvailableException.java
    │   │       └── PaymentException.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    │           ├── V1__create_users.sql
    │           ├── V2__create_installers.sql
    │           ├── V3__create_suppliers.sql
    │           ├── V4__create_jobs_quotes.sql
    │           ├── V5__create_parts.sql
    │           ├── V6__create_payments_escrow.sql
    │           ├── V7__create_maintenance.sql
    │           ├── V8__create_audits.sql
    │           ├── V9__create_reviews.sql
    │           └── V10__create_indexes.sql
    └── test/
        └── java/com/solarride/
            ├── service/
            │   ├── EscrowServiceTest.java         ← Testcontainers — real PostgreSQL
            │   ├── GeoMatchingServiceTest.java    ← Testcontainers — real PostGIS
            │   ├── PaymentPlanServiceTest.java
            │   └── JobLifecycleServiceTest.java
            └── controller/
                ├── AuthControllerTest.java
                ├── JobControllerTest.java
                └── PaymentControllerTest.java
```

---

## Module specifications

### M1 — Solar installation marketplace

**Purpose:** Customers discover, compare, and book certified installers by location.

**Key domain rules:**
- A `Job` begins in `DRAFT` status when a customer submits a request.
- A customer may request quotes from a maximum of 3 installers per job.
- Installers have 24 hours to respond to a quote request (SLA enforced by `SlaMonitorJob`).
- A `Job` moves to `CONFIRMED` only after escrow payment is received and verified.
- Solar system size is captured as a `SolarSize` enum on the `Job` entity:
  - `STARTER` → 1–2 kW (basic home)
  - `STANDARD` → 3–5 kW (family home)
  - `PREMIUM` → 6–10 kW (large home)
  - `COMMERCIAL` → 11 kW+ (business or estate)
- Job status transitions are strictly controlled by `JobLifecycleService` — no direct status
  field mutations anywhere else in the codebase.

**Geo-matching rules (`GeoMatchingService`):**
- Default search radius: 50 km from the customer's pinned property location.
- Installer ranking score = `(1 / distance_km) * 0.4 + rating * 0.4 + availability_score * 0.2`
- Use PostGIS `ST_DWithin` and `ST_Distance` — never calculate distances in Java.
- Only `ACTIVE` installers with valid certifications appear in results.

**API endpoints:**
```
POST   /api/v1/jobs                        Create job request
GET    /api/v1/jobs/{jobId}                Get job details
GET    /api/v1/jobs                        List customer's jobs
POST   /api/v1/jobs/{jobId}/quotes         Request quote from installer
GET    /api/v1/jobs/{jobId}/quotes         Get all quotes for a job
POST   /api/v1/jobs/{jobId}/quotes/{quoteId}/accept   Accept a quote
GET    /api/v1/installers/nearby           Geo-search installers (lat, lng, radius)
GET    /api/v1/installers/{installerId}    Get installer profile
```

---

### M2 — Parts procurement

**Purpose:** Auto-generate a Bill of Materials per confirmed job, broadcast to verified
suppliers, fulfil the order, and track delivery to the installer.

**Key domain rules:**
- A `BillOfMaterials` is created automatically when a `Job` moves to `CONFIRMED`.
- BOM is broadcast to all `ACTIVE` suppliers whose delivery coverage area intersects
  the job's location. Use PostGIS for this check.
- Suppliers have 6 hours to respond to a BOM broadcast with price + lead time.
- Best offer selection: lowest total price where lead time ≤ job start date minus 2 days.
- Parts payment is released from escrow to the supplier on installer confirmation of receipt.
- Supplier rating is updated after every fulfilled order.

**API endpoints:**
```
GET    /api/v1/jobs/{jobId}/bom                    Get BOM for a job
GET    /api/v1/supplier/rfq                        Supplier: list open RFQs
POST   /api/v1/supplier/rfq/{bomId}/quote          Supplier: submit parts quote
POST   /api/v1/jobs/{jobId}/parts/confirm-receipt  Installer: confirm parts received
GET    /api/v1/jobs/{jobId}/parts/tracking         Get delivery tracking status
```

---

### M3 — Maintenance & service

**Purpose:** Recurring maintenance plans and emergency on-demand service bookings
for post-installation customers.

**Plan tiers:**
| Tier | Frequency | Platform commission |
|---|---|---|
| `BASIC` | Annual | 15% |
| `STANDARD` | Bi-annual | 15% |
| `PREMIUM` | Quarterly | 15% |
| Emergency | On-demand | 12% |

**Key domain rules:**
- A maintenance plan is linked to a specific `Job` (the original installation).
- Monthly subscription fees are auto-charged via Flutterwave recurring charge on the
  billing anniversary date. This is triggered by `MaintenanceSchedulerJob`.
- Maintenance visits are assigned to the original installer (preferred) or the nearest
  available certified installer if original is unavailable.
- Each visit produces a `MaintenanceVisit` record with a digital report uploaded to S3.

**API endpoints:**
```
POST   /api/v1/jobs/{jobId}/maintenance/subscribe   Subscribe to maintenance plan
GET    /api/v1/maintenance/plans                    Customer: list active plans
POST   /api/v1/maintenance/visits/{visitId}/report  Installer: upload visit report
GET    /api/v1/maintenance/schedule                 Get upcoming maintenance schedule
POST   /api/v1/maintenance/emergency                Book emergency service
```

---

### M4 — Audit & inspection

**Purpose:** Independent third-party system performance audits assigned to certified
auditors (distinct credential from installers).

**Audit types:** `PERFORMANCE`, `COMPLIANCE`, `PRE_PURCHASE`, `INSURANCE`

**Key domain rules:**
- Auditors are a separate user role (`AUDITOR`) with separate certification requirements.
- An `AuditRequest` is assigned to the nearest available certified auditor.
- Auditor must upload an `AuditReport` (PDF in S3) within 48 hours of site visit
  (SLA enforced by `SlaMonitorJob`).
- Platform commission: 18% of audit fee.
- If deficiencies are found in the report, the platform auto-prompts the customer to
  book a remediation job through M1.

**API endpoints:**
```
POST   /api/v1/audits                              Request audit
GET    /api/v1/audits/{auditId}                    Get audit details
POST   /api/v1/audits/{auditId}/report             Auditor: submit report (PDF upload)
GET    /api/v1/audits/{auditId}/report              Download audit report
GET    /api/v1/auditor/assignments                 Auditor: list assigned audits
```

---

### M5 — Installer registry & vetting

**Purpose:** Manage certified installer onboarding, credential verification, geographic
indexing, and dynamic rating/badge management.

**Key domain rules:**
- New installer status: `PENDING_VERIFICATION` → admin reviews within 5 business days.
- On approval: status → `ACTIVE`, badge → `NEW_INSTALLER`.
- Badge promotion rules (enforced nightly by a scheduled job):
  - `STANDARD`: 5+ completed jobs with rating ≥ 3.5
  - `TOP_INSTALLER`: 50+ completed jobs with rating ≥ 4.5
- Auto-suspension trigger: rating < 3.5 for 3 consecutive completed jobs.
- Annual re-verification: Quartz job flags installers whose certifications expire within
  30 days; they receive email + SMS reminders.
- Installer location is stored as a PostGIS `Point` (WGS84). Service area stored as a
  PostGIS `Geometry` (polygon or circle).

**Required documents (stored in S3, references in DB):**
- Government-issued ID
- Solar professional certification (NASENI, NABCEP, or state-recognised equivalent)
- Business registration certificate (CAC)
- Proof of liability insurance

**API endpoints:**
```
POST   /api/v1/installer/register              Submit installer application
POST   /api/v1/installer/documents             Upload verification documents
GET    /api/v1/installer/profile               Get own profile
PUT    /api/v1/installer/profile               Update profile
PUT    /api/v1/installer/availability          Toggle availability
GET    /api/v1/admin/installers/pending        Admin: list pending applications
POST   /api/v1/admin/installers/{id}/approve  Admin: approve installer
POST   /api/v1/admin/installers/{id}/reject   Admin: reject with reason
```

---

### M6 — Escrow & payment engine

> **This is the most critical module. All methods that touch money MUST be
> `@Transactional`. Write Testcontainers integration tests for every method.
> Never mock the database in escrow tests.**

**Payment plans:**
- `OUTRIGHT`: Full amount paid at booking. 0% surcharge.
- `THREE_INSTALMENT`: Amount split into 3 equal payments with 15% interest added to
  the total before splitting.
  - Instalment 1: at booking (job moves to `CONFIRMED`)
  - Instalment 2: at mid-installation milestone (installer marks `MOUNTING_COMPLETE`)
  - Instalment 3: at job completion (before escrow release)

**Instalment interest calculation:**
```java
// In PaymentPlanService.java
BigDecimal totalWithInterest = jobValue.multiply(BigDecimal.valueOf(1.15));
BigDecimal instalmentAmount = totalWithInterest.divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
```

**Always use `BigDecimal` for all monetary values. Never use `double` or `float`.**

**Escrow release flow:**
1. Installer marks job `COMPLETED` and uploads evidence.
2. `EscrowReleaseJob` schedules a 48-hour countdown (stored in Quartz trigger).
3. If no dispute is raised in 48 hours: auto-release fires.
4. If dispute raised: job status → `DISPUTED`. Quartz trigger is cancelled. Admin resolves.
5. On release:
   - Parts supplier receives: `partsOrderValue - (partsOrderValue * 0.05)`
   - Installer receives: `(jobValue - partsOrderValue) - ((jobValue - partsOrderValue) * commissionRate)`
   - Platform retains: supplier commission + installer commission
   - All three disbursements happen in one `@Transactional` method.

**Commission rates:**
```java
public enum CommissionRate {
    STANDARD_INSTALL(0.12),
    TOP_INSTALLER_INSTALL(0.10),
    PARTS_PROCUREMENT(0.05),
    MAINTENANCE_SUBSCRIPTION(0.15),
    AUDIT_INSPECTION(0.18),
    EMERGENCY_RESPONSE(0.12);
}
```

**Flutterwave integration:**
- Use Flutterwave Split API to configure sub-accounts for installers and suppliers.
- Verify every webhook from Flutterwave using HMAC-SHA256 signature validation
  before updating any payment record.
- Idempotency: check `transactionReference` uniqueness before processing any payment
  event. Flutterwave webhooks can fire more than once.

**API endpoints:**
```
POST   /api/v1/payments/initiate           Initiate payment (returns Flutterwave link)
POST   /api/v1/payments/webhook            Flutterwave webhook receiver
GET    /api/v1/payments/escrow/{jobId}     Get escrow status
POST   /api/v1/payments/instalment/{id}/pay  Pay next instalment
POST   /api/v1/jobs/{jobId}/dispute        Raise dispute (opens 72-hr admin window)
POST   /api/v1/admin/disputes/{id}/resolve Admin: resolve dispute
GET    /api/v1/payments/history            Customer: payment history
GET    /api/v1/installer/payouts           Installer: payout history
```

---

### M7 — Analytics & admin dashboard

**Purpose:** Real-time operational visibility for platform operators covering revenue,
quality, SLA compliance, and geographic demand.

**Key metrics computed by `AnalyticsService`:**
- Gross Transaction Value (GTV) — daily, weekly, monthly
- Net platform revenue by commission stream
- Jobs by status and by geographic zone (Lagos LGA level)
- Installer league table: rating, job count, dispute rate, revenue generated
- Supplier performance: fill rate, on-time delivery %, defect reports
- Customer metrics: new vs returning, average job value, maintenance attach rate
- SLA compliance: % of quotes responded to within 24 hrs, % of audits delivered within 48 hrs
- Active disputes: open count, average resolution time

**Implementation notes:**
- Analytics queries run against a read replica of PostgreSQL (configure in `application-prod.yml`).
- Heavy aggregation queries are cached in Redis with a 5-minute TTL.
- Metabase is connected directly to the read replica for non-technical team reports —
  do not build custom BI charts for operational queries Metabase can handle.

**API endpoints:**
```
GET    /api/v1/admin/analytics/revenue          Revenue overview
GET    /api/v1/admin/analytics/jobs             Job pipeline metrics
GET    /api/v1/admin/analytics/installers       Installer performance table
GET    /api/v1/admin/analytics/suppliers        Supplier performance table
GET    /api/v1/admin/analytics/customers        Customer metrics
GET    /api/v1/admin/analytics/geo              Geographic demand heatmap data
GET    /api/v1/admin/analytics/sla              SLA compliance report
GET    /api/v1/admin/disputes                   Dispute log (open + resolved)
```

---

## Authentication & authorisation

**JWT configuration:**
- Access token TTL: 15 minutes
- Refresh token TTL: 30 days (stored in Redis, invalidated on logout)
- Algorithm: HS256 with secret from environment variable `JWT_SECRET`
- Claims: `userId`, `email`, `role`, `status`

**Role-based access control:**

| Role | Permitted modules |
|---|---|
| `CUSTOMER` | M1 (booking), M3 (maintenance), M4 (audit), M6 (payments), reviews |
| `INSTALLER` | M1 (job acceptance), M2 (parts receipt), M3 (visits), own profile |
| `SUPPLIER` | M2 (RFQ, fulfilment), own profile |
| `AUDITOR` | M4 (audit assignments + reports), own profile |
| `ADMIN` | All modules + M5 vetting + M7 analytics + dispute resolution |

**Apply `@PreAuthorize` at the service layer, not just the controller layer.**

Example:
```java
@PreAuthorize("hasRole('ADMIN') or @jobSecurityService.isJobOwner(#jobId, authentication)")
public JobResponse getJob(UUID jobId) { ... }
```

---

## Notification rules

Every critical platform event fires **both** a push notification (FCM) AND an SMS (Termii).
Never rely on push alone — Nigerian mobile networks drop FCM messages.

```
NotificationOrchestrator.notify(userId, event) {
    pushNotificationService.send(userId, event);   // async, non-blocking
    smsService.send(userPhone, event);             // async, non-blocking
}
```

**Events that trigger notifications:**

| Event | Recipients |
|---|---|
| Quote requested | Installer |
| Quote submitted | Customer |
| Job confirmed + payment received | Installer + Customer |
| BOM broadcast | All matching suppliers |
| Parts order confirmed | Installer |
| Parts delivered | Installer |
| Job marked complete | Customer |
| Dispute raised | Admin + Installer |
| Dispute resolved | Customer + Installer |
| Escrow released | Installer + Supplier |
| Instalment due (3 days before) | Customer |
| Maintenance visit scheduled | Customer + Installer |
| Certification expiry (30 days) | Installer |
| SLA breach warning | Admin |

---

## Database conventions

- All primary keys: `UUID` type (use `@GeneratedValue(strategy = GenerationType.UUID)`)
- All timestamps: `Instant` (stored as `TIMESTAMPTZ` in PostgreSQL)
- All monetary values: `BigDecimal` mapped to `NUMERIC(19,4)` in PostgreSQL
- All geographic points: `org.locationtech.jts.geom.Point` mapped via Hibernate Spatial
- Soft deletes: use `deletedAt Instant` field — never hard delete users, jobs, or payments
- Audit fields on every entity: `createdAt`, `updatedAt`, `createdBy` (use Spring Data Auditing)
- Table names: `snake_case` plural (e.g. `installer_certifications`, `escrow_accounts`)
- Column names: `snake_case`

**Flyway migration naming:** `V{number}__{description}.sql`
Always write both `up` and add a comment block at the top of each migration with:
- What it creates/modifies
- Why
- Date

---

## Code generation rules for Claude Code

When generating code for this project, always follow these rules:

1. **Use records for DTOs** — all request and response DTOs are Java records with
   Bean Validation annotations on request records.

2. **Use `@Transactional` on all service methods that write to the database** —
   especially everything in `EscrowService`, `PaymentPlanService`, and `PayoutService`.

3. **Never expose JPA entities directly in API responses** — always map to a DTO
   in the service layer. Use MapStruct for mapping.

4. **BigDecimal for all money** — never `double`, never `float`, never `long` for amounts.

5. **Use `Optional` correctly** — never call `.get()` without `.isPresent()` check.
   Prefer `.orElseThrow(() -> new ResourceNotFoundException(...))`.

6. **Logging** — use SLF4J with `@Slf4j` (Lombok). Log at `INFO` for all state
   transitions, `WARN` for SLA approaching, `ERROR` for payment failures. Never log
   full payment card numbers, tokens, or passwords.

7. **Tests use Testcontainers** — for any test that touches the database, use
   `@Testcontainers` with a real PostgreSQL+PostGIS image. No H2 in-memory database
   — PostGIS extensions are not available in H2.

8. **Webhook endpoints are unauthenticated but signature-verified** — Flutterwave
   webhooks do not carry a JWT. Verify the `verif-hash` header against
   `FLUTTERWAVE_SECRET_HASH` environment variable before processing.

9. **Quartz jobs are clustered** — configure Quartz with JDBC job store so scheduled
   jobs survive application restart and work correctly when multiple instances run.

10. **Environment variables for all secrets** — no secrets in `application.yml`.
    Use `${VARIABLE_NAME}` references. All secrets listed in `.env.example`.

---

## Environment variables

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/solarride
DB_USERNAME=solarride_user
DB_PASSWORD=

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# JWT
JWT_SECRET=
JWT_ACCESS_TTL_MINUTES=15
JWT_REFRESH_TTL_DAYS=30

# Flutterwave
FLUTTERWAVE_PUBLIC_KEY=
FLUTTERWAVE_SECRET_KEY=
FLUTTERWAVE_SECRET_HASH=
FLUTTERWAVE_BASE_URL=https://api.flutterwave.com/v3

# Paystack (fallback)
PAYSTACK_SECRET_KEY=
PAYSTACK_BASE_URL=https://api.paystack.co

# Google Maps
GOOGLE_MAPS_API_KEY=

# Firebase (FCM)
FIREBASE_PROJECT_ID=
FIREBASE_SERVICE_ACCOUNT_JSON=

# Termii (SMS)
TERMII_API_KEY=
TERMII_SENDER_ID=SolarRide
TERMII_BASE_URL=https://api.ng.termii.com

# AWS S3
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=af-south-1
AWS_S3_BUCKET=solarride-documents

# Sentry
SENTRY_DSN=

# App
APP_BASE_URL=https://api.solarride.ng
CORS_ALLOWED_ORIGINS=https://app.solarride.ng,https://supplier.solarride.ng
```

---

## Build & run locally

```bash
# Start dependencies
docker-compose up -d

# Run database migrations
./mvnw flyway:migrate

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test

# Run with Testcontainers integration tests
./mvnw verify
```

**`docker-compose.yml` services:**
- `postgres` — PostgreSQL 16 + PostGIS 3.4 on port 5432
- `redis` — Redis 7 on port 6379
- `swagger` — SpringDoc Swagger UI proxied at http://localhost:8080/swagger-ui.html

---

## Implementation order (follow this sequence)

Build in this exact order. Each step maps to a phase in the platform flow above.
Do not skip ahead — later steps depend on earlier ones being correct.

```
━━━ PLATFORM FLOW PHASE 1 — ONBOARDING ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 1:  Flyway migrations V1–V10 (all tables, indexes, PostGIS extensions)
Step 2:  All JPA domain entities + enums
Step 3:  Spring Security config + JWT filter + role-based access
Step 4:  OtpService (Termii integration) + email verification
Step 5:  Customer signup/signin flow
         → CustomerOnboardingService
         → POST /api/v1/auth/customer/register
         → POST /api/v1/auth/login
         → POST /api/v1/auth/verify-otp
Step 6:  Installer signup/signin + document upload (S3)
         → InstallerOnboardingService
         → POST /api/v1/installer/register
         → POST /api/v1/installer/documents
         → Admin approval endpoints
Step 7:  Supplier signup/signin + document upload (S3)
         → SupplierOnboardingService
         → POST /api/v1/supplier/register
         → Admin approval endpoints
Step 8:  GeoMatchingService (PostGIS ST_DWithin radius search + ranking formula)
         → GET /api/v1/installers/nearby

━━━ PLATFORM FLOW PHASE 2 — JOB REQUEST & SOLAR SIZING ━━━━━━━━━━━━━━━━━━━━━

Step 9:  Job creation with SolarSize selection
         → JobService.createJob()
         → POST /api/v1/jobs
         → SolarSize enum: STARTER / STANDARD / PREMIUM / COMMERCIAL
Step 10: CostEstimationService (indicative price range by SolarSize + location)
Step 11: Quote request/submission flow + 24-hour SLA countdown
         → QuoteService.requestQuote() + QuoteService.submitQuote()
         → POST /api/v1/jobs/{jobId}/quotes
         → SlaMonitorJob for 24-hour quote response tracking
Step 12: Quote acceptance + job status → QUOTED
         → QuoteService.acceptQuote()

━━━ PLATFORM FLOW PHASE 3 — PAYMENT SELECTION & ESCROW ━━━━━━━━━━━━━━━━━━━━━

Step 13: Payment plan selection (OUTRIGHT vs THREE_INSTALMENT)
         → PaymentPlanService.selectPlan()
         → 15% interest calculation for THREE_INSTALMENT (BigDecimal only)
         → Instalment entity creation
Step 14: Flutterwave payment initiation + webhook receiver
         → FlutterwaveClient.initializeTransaction()
         → POST /api/v1/payments/webhook (signature-verified, unauthenticated)
Step 15: EscrowService — fund escrow on payment confirmation [@Transactional]
         → EscrowAccount credited, Transaction record created
         → Job status → CONFIRMED
Step 16: InstalmentReminderJob (Quartz) for THREE_INSTALMENT milestone triggers

━━━ PLATFORM FLOW PHASE 4 — PARTS PROCUREMENT ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 17: BomGenerationService — auto-generate BOM on CONFIRMED
         → BillOfMaterials + BomItem entities created
Step 18: PartsOrderService — broadcast BOM to suppliers via PostGIS coverage check
         → Supplier 6-hour response window + SLA tracking
Step 19: Supplier quote submission + best-offer selection
         → POST /api/v1/supplier/rfq/{bomId}/quote
Step 20: PartsOrder confirmation + dispatch + delivery tracking
         → POST /api/v1/jobs/{jobId}/parts/confirm-receipt
         → Parts payment released from escrow on delivery [@Transactional]

━━━ PLATFORM FLOW PHASE 5 — INSTALLATION EXECUTION ━━━━━━━━━━━━━━━━━━━━━━━━━

Step 21: Job milestone tracking (JobMilestone entity + WebSocket broadcasts)
         → POST /api/v1/jobs/{jobId}/milestones/{milestone}
         → Real-time status via Socket.io equivalent (Spring WebSocket + STOMP)
Step 22: THREE_INSTALMENT Instalment 2 trigger at MOUNTING_COMPLETE milestone
         → InstalmentService.triggerInstalment(jobId, 2)
Step 23: Job completion + evidence upload (S3 photos + handover sheet)
         → POST /api/v1/jobs/{jobId}/complete
         → Job status → COMPLETED
         → THREE_INSTALMENT Instalment 3 triggered

━━━ PLATFORM FLOW PHASE 6 — VERIFICATION, DISPUTE & PAYMENT RELEASE ━━━━━━━━━

Step 24: EscrowReleaseJob (Quartz JDBC store — survives restarts)
         → Fires at completedAt + 48 hours if no dispute
         → EscrowService.releaseEscrow(jobId) [@Transactional]
         → Atomic: supplier payout + installer payout + commission retained
         → Flutterwave split payments fired
Step 25: Dispute flow
         → POST /api/v1/jobs/{jobId}/dispute
         → Quartz trigger cancelled on dispute raise
         → Admin resolution endpoints (FULL_RELEASE / PARTIAL / FULL_REFUND)
Step 26: Review + rating submission
         → ReviewService.submitReview()
         → Installer averageRating recalculated
         → InstallerBadgePromotionJob (nightly Quartz)

━━━ PLATFORM FLOW PHASE 7 — POST-INSTALLATION SERVICES ━━━━━━━━━━━━━━━━━━━━━

Step 27: Maintenance plan subscription + recurring billing (Flutterwave)
         → MaintenanceService.subscribePlan()
         → MaintenanceSchedulerJob for automated visit scheduling
Step 28: Maintenance visit execution + report upload (S3)
         → POST /api/v1/maintenance/visits/{visitId}/report
Step 29: Audit request + auditor assignment + report delivery (S3)
         → AuditService.requestAudit() + AuditService.submitReport()
         → 48-hour SLA enforced by SlaMonitorJob
Step 30: Post-installation upsell prompt + 30-day reminder Quartz job

━━━ PLATFORM FLOW PHASE 8 — ANALYTICS & ADMIN ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Step 31: NotificationOrchestrator — FCM + Termii SMS for all platform events
         (build this before Phase 2 in practice; wire in events as each step completes)
Step 32: AnalyticsService + admin dashboard API endpoints (M7)
         → Read replica queries + Redis caching (5-min TTL)
Step 33: SlaMonitorJob — comprehensive SLA breach detection + admin alerts
Step 34: CertificationExpiryReminderJob — 30-day advance warning to installers
Step 35: Admin dispute resolution dashboard + bulk reporting endpoints
```

---

## Key architectural decisions

**Why PostGIS over application-level geo calculations:**
All distance calculations, radius searches, and coverage area intersections happen
inside PostgreSQL using PostGIS. This keeps geo logic close to the data, avoids
N+1 query problems, and uses spatial indexes (GIST) for performance.

**Why Quartz over Spring `@Scheduled`:**
Quartz persists job state to the database (JDBC job store). If the application
restarts mid-countdown on a 48-hour escrow release timer, the timer survives.
Spring `@Scheduled` is in-memory only — a restart loses all pending timers,
which would leave funds stuck in escrow indefinitely.

**Why BigDecimal everywhere for money:**
IEEE 754 floating-point arithmetic cannot represent all decimal fractions exactly.
`0.1 + 0.2 = 0.30000000000000004` in doubles. At ₦2,500,000 per job,
floating-point rounding errors translate to real naira discrepancies in payouts.
Use `NUMERIC(19,4)` in PostgreSQL and `BigDecimal` in Java for all monetary values.

**Why both FCM and SMS for every notification:**
Push notification delivery rates on Nigerian mobile networks (MTN, Glo, Airtel,
9mobile) are unreliable, especially in low-signal areas or when data is off.
SMS delivers reliably regardless. For time-sensitive SLA events, failure to notify
an installer within the response window causes platform-level SLA breaches.

---

*Last updated: May 2026 | SolarRide v1.0 | Java 21 + Spring Boot 3.3*
