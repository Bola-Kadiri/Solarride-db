-- ============================================================
-- Migration : V7__create_payments_escrow.sql
-- What      : Creates escrow_accounts, transactions, and
--             instalments tables. These are the financial core
--             of the platform.
-- Why       : All money flows through escrow before reaching
--             installer or supplier. The transaction table is
--             an append-only ledger. Instalments support the
--             THREE_INSTALMENT payment plan.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE escrow_accounts (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id      UUID          NOT NULL UNIQUE REFERENCES jobs(id),
    balance     NUMERIC(19,4) NOT NULL DEFAULT 0.0000,
    status      VARCHAR(30)   NOT NULL DEFAULT 'OPEN',
    released_at TIMESTAMPTZ,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by  UUID
);

COMMENT ON COLUMN escrow_accounts.status IS 'OPEN | RELEASED | REFUNDED | DISPUTED';

CREATE TABLE transactions (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                UUID          REFERENCES jobs(id),
    escrow_account_id     UUID          REFERENCES escrow_accounts(id),
    transaction_reference VARCHAR(255)  NOT NULL UNIQUE,
    type                  VARCHAR(50)   NOT NULL,
    amount                NUMERIC(19,4) NOT NULL,
    status                VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    direction             VARCHAR(10)   NOT NULL,
    payer_id              UUID,
    payee_id              UUID,
    flutterwave_tx_id     VARCHAR(255),
    metadata              TEXT,
    deleted_at            TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by            UUID
);

COMMENT ON COLUMN transactions.type IS
    'ESCROW_FUND | ESCROW_RELEASE | INSTALMENT | SUPPLIER_PAYOUT | '
    'INSTALLER_PAYOUT | REFUND | COMMISSION';
COMMENT ON COLUMN transactions.direction IS 'CREDIT | DEBIT';
COMMENT ON COLUMN transactions.status    IS 'PENDING | SUCCESS | FAILED | REVERSED';

CREATE TABLE instalments (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id            UUID          NOT NULL REFERENCES jobs(id),
    instalment_number INTEGER       NOT NULL,
    amount            NUMERIC(19,4) NOT NULL,
    status            VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    due_at            TIMESTAMPTZ,
    paid_at           TIMESTAMPTZ,
    transaction_id    UUID          REFERENCES transactions(id),
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by        UUID
);

COMMENT ON COLUMN instalments.status IS 'PENDING | PAID | OVERDUE';