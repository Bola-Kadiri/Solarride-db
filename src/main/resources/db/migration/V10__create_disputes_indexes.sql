-- ============================================================
-- Migration : V10__create_disputes_indexes.sql
-- What      : Creates the disputes table and all performance
--             indexes (spatial GIST + regular B-tree).
-- Why       : Disputes must freeze escrow instantly; they need
--             their own table so the status and resolution are
--             auditable. All indexes created last to avoid
--             slowing inserts during earlier migrations.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE disputes (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id      UUID        NOT NULL UNIQUE REFERENCES jobs(id),
    customer_id UUID        NOT NULL REFERENCES users(id),
    description TEXT        NOT NULL,
    status      VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution  VARCHAR(30),
    resolved_by UUID        REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    admin_notes TEXT,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  UUID
);

COMMENT ON COLUMN disputes.status     IS 'OPEN | UNDER_REVIEW | RESOLVED';
COMMENT ON COLUMN disputes.resolution IS
    'FULL_RELEASE | PARTIAL_REFUND | FULL_REFUND';

-- ── Spatial indexes (GIST) ────────────────────────────────────────────────
CREATE INDEX idx_installers_location     ON installers USING GIST (location);
CREATE INDEX idx_installers_service_area ON installers USING GIST (service_area);
CREATE INDEX idx_suppliers_coverage_area ON suppliers  USING GIST (coverage_area);
CREATE INDEX idx_jobs_property_location  ON jobs       USING GIST (property_location);

-- ── Users ────────────────────────────────────────────────────────────────
CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_phone  ON users (phone);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_status ON users (status);

-- ── Installers ───────────────────────────────────────────────────────────
CREATE INDEX idx_installers_user_id  ON installers (user_id);
CREATE INDEX idx_installers_badge    ON installers (badge);
CREATE INDEX idx_installers_available ON installers (is_available);

-- ── Supplier certifications ───────────────────────────────────────────────
CREATE INDEX idx_installer_certs_installer_id ON installer_certifications (installer_id);

-- ── Suppliers ────────────────────────────────────────────────────────────
CREATE INDEX idx_suppliers_user_id ON suppliers (user_id);

-- ── Jobs ─────────────────────────────────────────────────────────────────
CREATE INDEX idx_jobs_customer_id  ON jobs (customer_id);
CREATE INDEX idx_jobs_installer_id ON jobs (installer_id);
CREATE INDEX idx_jobs_status       ON jobs (status);

-- ── Quotes ───────────────────────────────────────────────────────────────
CREATE INDEX idx_quotes_job_id       ON quotes (job_id);
CREATE INDEX idx_quotes_installer_id ON quotes (installer_id);
CREATE INDEX idx_quotes_status       ON quotes (status);

-- ── Parts ────────────────────────────────────────────────────────────────
CREATE INDEX idx_bom_job_id          ON bills_of_materials (job_id);
CREATE INDEX idx_bom_items_bom_id    ON bom_items (bom_id);
CREATE INDEX idx_parts_orders_job_id ON parts_orders (job_id);
CREATE INDEX idx_rfq_quotes_bom_id   ON supplier_rfq_quotes (bom_id);

-- ── Payments ─────────────────────────────────────────────────────────────
CREATE INDEX idx_escrow_job_id            ON escrow_accounts (job_id);
CREATE INDEX idx_transactions_reference   ON transactions (transaction_reference);
CREATE INDEX idx_transactions_job_id      ON transactions (job_id);
CREATE INDEX idx_instalments_job_id       ON instalments (job_id);

-- ── Maintenance ───────────────────────────────────────────────────────────
CREATE INDEX idx_maintenance_plans_job_id ON maintenance_plans (job_id);
CREATE INDEX idx_maintenance_visits_plan  ON maintenance_visits (maintenance_plan_id);

-- ── Audits ────────────────────────────────────────────────────────────────
CREATE INDEX idx_audit_requests_job_id ON audit_requests (job_id);
CREATE INDEX idx_reviews_installer_id  ON reviews (installer_id);
CREATE INDEX idx_disputes_job_id       ON disputes (job_id);