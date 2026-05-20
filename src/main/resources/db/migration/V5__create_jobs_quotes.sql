-- ============================================================
-- Migration : V5__create_jobs_quotes.sql
-- What      : Creates jobs, job_milestones, job_evidences, and
--             quotes tables. The jobs table is the central
--             entity of the platform — all other tables
--             eventually reference it.
-- Why       : Jobs drive the state machine. Milestones allow
--             granular progress tracking and trigger instalment
--             payments. Evidences satisfy the 3-photo + handover
--             sheet requirement for job completion.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE jobs (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id          UUID          NOT NULL REFERENCES users(id),
    installer_id         UUID          REFERENCES installers(id),
    solar_size           VARCHAR(20)   NOT NULL,
    status               VARCHAR(30)   NOT NULL DEFAULT 'DRAFT',
    payment_plan         VARCHAR(30),
    property_type        VARCHAR(100),
    property_location    GEOMETRY(POINT, 4326),
    property_address     TEXT,
    preferred_start_date DATE,
    estimated_cost_min   NUMERIC(19,4),
    estimated_cost_max   NUMERIC(19,4),
    job_value            NUMERIC(19,4),
    parts_order_value    NUMERIC(19,4),
    completed_at         TIMESTAMPTZ,
    deleted_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by           UUID
);

COMMENT ON COLUMN jobs.solar_size IS 'STARTER | STANDARD | PREMIUM | COMMERCIAL';
COMMENT ON COLUMN jobs.status     IS
    'DRAFT | QUOTED | CONFIRMED | IN_PROGRESS | COMPLETED | PAID | '
    'DISPUTED | PARTIALLY_REFUNDED | REFUNDED | CANCELLED | CLOSED';
COMMENT ON COLUMN jobs.payment_plan IS 'OUTRIGHT | THREE_INSTALMENT';

CREATE TABLE job_milestones (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id       UUID        NOT NULL REFERENCES jobs(id),
    milestone    VARCHAR(50) NOT NULL,
    recorded_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   UUID
);

COMMENT ON COLUMN job_milestones.milestone IS
    'SITE_SURVEY_COMPLETE | MOUNTING_COMPLETE | PANELS_FITTED | '
    'WIRING_COMPLETE | SYSTEM_TESTED | HANDOVER_COMPLETE';

CREATE TABLE job_evidences (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id        UUID         NOT NULL REFERENCES jobs(id),
    s3_key        VARCHAR(500) NOT NULL,
    evidence_type VARCHAR(50)  NOT NULL,
    description   TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    UUID
);

COMMENT ON COLUMN job_evidences.evidence_type IS 'PHOTO | HANDOVER_SHEET';

CREATE TABLE quotes (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                UUID         NOT NULL REFERENCES jobs(id),
    installer_id          UUID         NOT NULL REFERENCES installers(id),
    status                VARCHAR(30)  NOT NULL DEFAULT 'REQUESTED',
    labour_cost           NUMERIC(19,4),
    estimated_parts_cost  NUMERIC(19,4),
    total_cost            NUMERIC(19,4),
    proposed_start_date   DATE,
    proposed_end_date     DATE,
    notes                 TEXT,
    sla_deadline_at       TIMESTAMPTZ,
    deleted_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by            UUID
);

COMMENT ON COLUMN quotes.status IS 'REQUESTED | SUBMITTED | ACCEPTED | DECLINED | EXPIRED';