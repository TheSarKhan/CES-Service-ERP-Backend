-- V1__init_schema.sql
-- Purpose: Create the ces_service schema and required PostgreSQL extensions.
-- All subsequent objects are created explicitly under the ces_service schema
-- (every object name is prefixed with ces_service.).

CREATE SCHEMA IF NOT EXISTS ces_service;

-- pgcrypto provides gen_random_uuid() used as the default for all UUID primary keys.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- pg_trgm provides trigram indexes for full-text / fuzzy "search" features (SRS 2.3, 6.3).
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- NOTE: Extensions are created in the default (public) schema, which is acceptable.
-- The application connects with search_path including ces_service (Flyway schemas: ces_service),
-- but every migration below uses fully-qualified ces_service.<object> names to be explicit
-- and independent of the runtime search_path.
