-- V11: Add human-readable address fields to users, installers, and suppliers.
-- Customers store their home address on the users table.
-- Installers store their shop/office address on the installers table.
-- Suppliers store their warehouse/shop address on the suppliers table.
-- Geo-matching is driven exclusively by the customer's job location — the
-- installer's service radius is no longer set by the installer.
-- Date: 2026-05-20

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS home_address TEXT;

ALTER TABLE installers
    ADD COLUMN IF NOT EXISTS shop_address TEXT;

ALTER TABLE suppliers
    ADD COLUMN IF NOT EXISTS shop_address TEXT;