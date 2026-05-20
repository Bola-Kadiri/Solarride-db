-- ============================================================
-- Migration : V1__init.sql
-- What      : Enables the PostGIS extension required for all
--             geospatial columns (installer location, supplier
--             coverage area, job property location).
-- Why       : PostGIS must be present before any migration that
--             defines geometry/geography columns or spatial indexes.
--             This migration must always run first.
-- Date      : 2026-05-20
-- ============================================================

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;