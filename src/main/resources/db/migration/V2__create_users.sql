-- ============================================================
-- Migration : V2__create_users.sql
-- What      : Creates the users table — the root identity table
--             shared by all roles (CUSTOMER, INSTALLER, SUPPLIER,
--             AUDITOR, ADMIN). Role-specific data lives in
--             dedicated tables that reference this one.
-- Why       : All platform users authenticate via this table.
--             Role enum, status enum, and OTP/email verification
--             flags are stored here to keep auth fast.
-- Date      : 2026-05-20
-- ============================================================

CREATE TABLE users (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL UNIQUE,
    phone             VARCHAR(20)  NOT NULL UNIQUE,
    first_name        VARCHAR(100) NOT NULL,
    last_name         VARCHAR(100) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    email_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    phone_verified    BOOLEAN      NOT NULL DEFAULT FALSE,
    fcm_token         VARCHAR(500),
    deleted_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by        UUID
);

COMMENT ON TABLE  users IS 'Platform identity table for all user roles';
COMMENT ON COLUMN users.role   IS 'CUSTOMER | INSTALLER | SUPPLIER | AUDITOR | ADMIN';
COMMENT ON COLUMN users.status IS 'PENDING | ACTIVE | SUSPENDED';