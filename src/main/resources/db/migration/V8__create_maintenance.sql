-- ============================================================
-- Migration : V8__create_maintenance.sql
-- What      : Creates maintenance_plans and maintenance_visits
--             tables.
-- Why       : Recurring maintenance is a separate revenue stream
--             (15% commission). Plans are linked to the original
--             installation job so the same property's history
--             is always accessible.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE maintenance_plans (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                      UUID        NOT NULL REFERENCES jobs(id),
    customer_id                 UUID        NOT NULL REFERENCES users(id),
    tier                        VARCHAR(30) NOT NULL,
    status                      VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    next_visit_at               TIMESTAMPTZ,
    billing_anchor_date         DATE,
    flutterwave_subscription_id VARCHAR(255),
    deleted_at                  TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  UUID
);

COMMENT ON COLUMN maintenance_plans.tier   IS 'BASIC | STANDARD | PREMIUM | EMERGENCY';
COMMENT ON COLUMN maintenance_plans.status IS 'ACTIVE | PAUSED | CANCELLED';

CREATE TABLE maintenance_visits (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    maintenance_plan_id  UUID        NOT NULL REFERENCES maintenance_plans(id),
    installer_id         UUID        NOT NULL REFERENCES installers(id),
    scheduled_at         TIMESTAMPTZ NOT NULL,
    completed_at         TIMESTAMPTZ,
    status               VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    report_s3_key        VARCHAR(500),
    findings             TEXT,
    requires_remediation BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           UUID
);

COMMENT ON COLUMN maintenance_visits.status IS 'SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED';