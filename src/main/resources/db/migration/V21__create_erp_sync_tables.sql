-- V21__create_erp_sync_tables.sql
-- Purpose: M13 — erp_sync_logs (SRS M13.2) with sync_type/direction/status CHECKs, payload JSONB.

CREATE TABLE ces_service.erp_sync_logs (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id      UUID          NOT NULL REFERENCES ces_service.branches(id),
    sync_type      VARCHAR(50)   NOT NULL
                   CHECK (sync_type IN ('CUSTOMER_CREATE','CUSTOMER_UPDATE',
                          'CUSTOMER_PULL','ACCOUNTING_PUSH')),
    direction      VARCHAR(20)   NOT NULL CHECK (direction IN ('TO_ERP','FROM_ERP')),
    entity_id      UUID          NULL,     -- CES Service entity UUID
    erp_entity_id  UUID          NULL,     -- ERP-side UUID
    status         VARCHAR(20)   NOT NULL CHECK (status IN ('SUCCESS','FAILED','PENDING')),
    attempt_count  INTEGER       NOT NULL DEFAULT 1,
    error_message  TEXT          NULL,
    payload        JSONB         NULL,     -- sent/received data
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by     UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by     UUID          NOT NULL REFERENCES ces_service.users(id)
);
