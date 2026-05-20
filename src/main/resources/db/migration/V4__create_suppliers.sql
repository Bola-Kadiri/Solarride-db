-- ============================================================
-- Migration : V4__create_suppliers.sql
-- What      : Creates the suppliers table (profile + PostGIS
--             coverage polygon), supplier_product_categories,
--             and supplier_documents tables.
-- Why       : Suppliers need a geospatial coverage area so BOM
--             broadcasts can use ST_Intersects to fan out only
--             to suppliers whose delivery area covers the job
--             location.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE suppliers (
    id                          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                     UUID           NOT NULL UNIQUE REFERENCES users(id),
    company_name                VARCHAR(255)   NOT NULL,
    registration_number         VARCHAR(100),
    tax_id                      VARCHAR(100),
    rep_name                    VARCHAR(255),
    coverage_area               GEOMETRY(GEOMETRY, 4326),
    delivery_lead_time_days     INTEGER,
    sla_accepted                BOOLEAN        NOT NULL DEFAULT FALSE,
    sla_accepted_at             TIMESTAMPTZ,
    average_rating              NUMERIC(3,2)   NOT NULL DEFAULT 0.00,
    flutterwave_subaccount_id   VARCHAR(255),
    deleted_at                  TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by                  UUID
);

CREATE TABLE supplier_product_categories (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id  UUID        NOT NULL REFERENCES suppliers(id),
    category     VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by   UUID
);

CREATE TABLE supplier_documents (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    supplier_id       UUID        NOT NULL REFERENCES suppliers(id),
    document_type     VARCHAR(50) NOT NULL,
    s3_key            VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by        UUID
);

COMMENT ON COLUMN supplier_documents.document_type IS
    'CAC_CERT | TAX_CLEARANCE | DIRECTOR_ID';