-- ============================================================
-- Migration : V3__create_installers.sql
-- What      : Creates the installers table (installer profile +
--             geo location) and installer_certifications table
--             (S3 document references for vetting).
-- Why       : Installer profile is separate from the core user
--             row so that geo queries (ST_DWithin) can hit a
--             narrow, spatially-indexed table without joining
--             user auth data.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE installers (
    id                          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID           NOT NULL UNIQUE REFERENCES users(id),
    company_name                VARCHAR(255),
    cac_number                  VARCHAR(100),
    tax_id                      VARCHAR(100),
    location                    GEOMETRY(POINT, 4326),
    service_radius_km           INTEGER        NOT NULL DEFAULT 50,
    service_area                GEOMETRY(GEOMETRY, 4326),
    badge                       VARCHAR(30)    NOT NULL DEFAULT 'NEW_INSTALLER',
    average_rating              NUMERIC(3,2)   NOT NULL DEFAULT 0.00,
    completed_jobs_count        INTEGER        NOT NULL DEFAULT 0,
    availability_score          NUMERIC(3,2)   NOT NULL DEFAULT 1.00,
    is_available                BOOLEAN        NOT NULL DEFAULT TRUE,
    sla_accepted                BOOLEAN        NOT NULL DEFAULT FALSE,
    sla_accepted_at             TIMESTAMPTZ,
    flutterwave_subaccount_id   VARCHAR(255),
    deleted_at                  TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by                  UUID
);

COMMENT ON COLUMN installers.badge IS 'NEW_INSTALLER | STANDARD | TOP_INSTALLER';

CREATE TABLE installer_certifications (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    installer_id      UUID        NOT NULL REFERENCES installers(id),
    document_type     VARCHAR(50) NOT NULL,
    s3_key            VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    expires_at        TIMESTAMPTZ,
    verified          BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        UUID
);

COMMENT ON COLUMN installer_certifications.document_type IS
    'SOLAR_LICENCE | LIABILITY_INSURANCE | GOVT_ID | CAC_CERTIFICATE';