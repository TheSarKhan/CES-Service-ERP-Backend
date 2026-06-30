-- V22__create_audit_log_table.sql
-- Purpose: M17 — audit_logs (SRS M17.1) + 4 indexes. ip_address is INET.
-- Immutable log: there is no created_by/updated_by FK (the system writes these rows itself),
-- branch_id and user_id are NULLABLE (system-level events), and rows are never deleted.

CREATE TABLE ces_service.audit_logs (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id       UUID          NULL,          -- NULL = system-level event
    user_id         UUID          NULL,          -- NULL = system (scheduled job)
    user_email      VARCHAR(255)  NULL,          -- snapshot: survives user deletion
    user_full_name  VARCHAR(255)  NULL,
    ip_address      INET          NULL,
    user_agent      TEXT          NULL,
    event_type      VARCHAR(100)  NOT NULL,
    module          VARCHAR(100)  NOT NULL,
    action          VARCHAR(100)  NOT NULL,      -- CREATE|READ|UPDATE|DELETE|BUSINESS
    entity_type     VARCHAR(100)  NULL,          -- WORK_ORDER|VEHICLE|USER...
    entity_id       UUID          NULL,
    entity_number   VARCHAR(100)  NULL,          -- WO-2026-0042 (human-readable)
    old_value       JSONB         NULL,
    new_value       JSONB         NULL,
    result          VARCHAR(20)   NOT NULL
                    CHECK (result IN ('SUCCESS','FAILED','DENIED')),
    error_message   TEXT          NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_branch_time ON ces_service.audit_logs(branch_id, created_at DESC);
CREATE INDEX idx_audit_user_time   ON ces_service.audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_entity      ON ces_service.audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_event_type  ON ces_service.audit_logs(event_type, created_at DESC);
