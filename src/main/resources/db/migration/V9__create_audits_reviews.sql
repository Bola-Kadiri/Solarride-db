-- ============================================================
-- Migration : V9__create_audits_reviews.sql
-- What      : Creates audit_requests, audit_reports, and
--             reviews tables.
-- Why       : Audits (18% commission) are independent of
--             installation jobs but reference them for property
--             context. Reviews drive installer badge promotion
--             and platform trust signals.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE audit_requests (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id             UUID          NOT NULL REFERENCES jobs(id),
    customer_id        UUID          NOT NULL REFERENCES users(id),
    auditor_id         UUID          REFERENCES users(id),
    audit_type         VARCHAR(30)   NOT NULL,
    status             VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    fee                NUMERIC(19,4),
    site_visit_at      TIMESTAMPTZ,
    report_deadline_at TIMESTAMPTZ,
    deleted_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by         UUID
);

COMMENT ON COLUMN audit_requests.audit_type IS
    'PERFORMANCE | COMPLIANCE | PRE_PURCHASE | INSURANCE';
COMMENT ON COLUMN audit_requests.status IS
    'PENDING | ASSIGNED | IN_PROGRESS | REPORT_SUBMITTED | COMPLETED | CANCELLED';

CREATE TABLE audit_reports (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_request_id  UUID         NOT NULL UNIQUE REFERENCES audit_requests(id),
    s3_key            VARCHAR(500) NOT NULL,
    summary           TEXT,
    deficiencies_found BOOLEAN     NOT NULL DEFAULT FALSE,
    submitted_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        UUID
);

CREATE TABLE reviews (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id       UUID        NOT NULL UNIQUE REFERENCES jobs(id),
    customer_id  UUID        NOT NULL REFERENCES users(id),
    installer_id UUID        NOT NULL REFERENCES installers(id),
    rating       INTEGER     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   UUID
);