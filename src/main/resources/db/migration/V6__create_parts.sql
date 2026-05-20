-- ============================================================
-- Migration : V6__create_parts.sql
-- What      : Creates bills_of_materials, bom_items,
--             parts_orders, and supplier_rfq_quotes tables.
-- Why       : Parts procurement is auto-triggered on job
--             CONFIRMED. The BOM drives supplier RFQ broadcasts
--             and the winning PartsOrder tracks delivery to the
--             installer before escrow releases parts payment.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE bills_of_materials (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                UUID        NOT NULL UNIQUE REFERENCES jobs(id),
    broadcast_deadline_at TIMESTAMPTZ,
    status                VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by            UUID
);

COMMENT ON COLUMN bills_of_materials.status IS 'PENDING | BROADCAST | FULFILLED | CANCELLED';

CREATE TABLE bom_items (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    bom_id        UUID         NOT NULL REFERENCES bills_of_materials(id),
    item_name     VARCHAR(255) NOT NULL,
    category      VARCHAR(100),
    quantity      INTEGER      NOT NULL DEFAULT 1,
    unit          VARCHAR(50),
    specification TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by    UUID
);

CREATE TABLE parts_orders (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    bom_id        UUID          NOT NULL REFERENCES bills_of_materials(id),
    supplier_id   UUID          NOT NULL REFERENCES suppliers(id),
    job_id        UUID          NOT NULL REFERENCES jobs(id),
    status        VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    total_price   NUMERIC(19,4) NOT NULL,
    delivery_date DATE,
    tracking_id   VARCHAR(255),
    dispatched_at TIMESTAMPTZ,
    delivered_at  TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by    UUID
);

COMMENT ON COLUMN parts_orders.status IS 'PENDING | CONFIRMED | DISPATCHED | DELIVERED';

CREATE TABLE supplier_rfq_quotes (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    bom_id                 UUID          NOT NULL REFERENCES bills_of_materials(id),
    supplier_id            UUID          NOT NULL REFERENCES suppliers(id),
    total_price            NUMERIC(19,4) NOT NULL,
    delivery_date          DATE          NOT NULL,
    availability_confirmed BOOLEAN       NOT NULL DEFAULT FALSE,
    notes                  TEXT,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by             UUID
);