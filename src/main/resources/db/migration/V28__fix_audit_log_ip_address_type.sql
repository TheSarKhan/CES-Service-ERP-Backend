-- V28__fix_audit_log_ip_address_type.sql
-- Purpose: audit_logs.ip_address was declared INET, but AuditLog.java (and every caller) has
-- always treated it as a plain string with no inet-specific validation. Binding a Java String
-- parameter into an INET column requires PgJDBC-level type coercion that Hibernate 6 does not
-- apply automatically, which fails at insert time (SQLState 42804) — this table had no writer
-- until InventoryAuditLogger became its first. Switch the column to VARCHAR to match how it's
-- actually used, rather than fighting driver/ORM type inference for a benefit nothing uses.

ALTER TABLE ces_service.audit_logs
    ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::varchar;
