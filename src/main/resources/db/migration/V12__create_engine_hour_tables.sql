-- V12__create_engine_hour_tables.sql
-- Purpose: M08 — engine_hour_logs (delta GENERATED), engine_hour_alerts (SRS M08.1) + index.
-- The vehicles.current_engine_hours column and the sync trigger are added in V13.

CREATE TABLE ces_service.engine_hour_logs (
    id               UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id        UUID           NOT NULL REFERENCES ces_service.branches(id),
    vehicle_id       UUID           NOT NULL REFERENCES ces_service.vehicles(id),
    hours_value      NUMERIC(10,1)  NOT NULL,         -- new engine-hour value
    previous_value   NUMERIC(10,1)  NULL,             -- previous reading (for audit)
    delta            NUMERIC(10,1)
        GENERATED ALWAYS AS (hours_value - previous_value) STORED,
    entry_type       VARCHAR(50)    NOT NULL DEFAULT 'MANUAL'
                     CHECK (entry_type IN ('MANUAL','MOBILE','INSPECTION')),
    is_rollover      BOOLEAN        NOT NULL DEFAULT FALSE,
    rollover_reason  TEXT           NULL,
    source_ref_id    UUID           NULL,   -- inspection_id or wo_id (origin)
    recorded_at      DATE           NOT NULL DEFAULT CURRENT_DATE,
    notes            TEXT           NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by       UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_by       UUID           NOT NULL REFERENCES ces_service.users(id)
);

CREATE INDEX idx_eh_logs_vehicle_date
    ON ces_service.engine_hour_logs(vehicle_id, recorded_at DESC);

CREATE TABLE ces_service.engine_hour_alerts (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id         UUID           NOT NULL REFERENCES ces_service.branches(id),
    vehicle_id        UUID           NOT NULL REFERENCES ces_service.vehicles(id),
    alert_type        VARCHAR(50)    NOT NULL
                      CHECK (alert_type IN ('INSPECTION_DUE','CUSTOM')),
    threshold_hours   NUMERIC(10,1)  NOT NULL,
    warning_before    NUMERIC(10,1)  NOT NULL DEFAULT 50,
    is_active         BOOLEAN        NOT NULL DEFAULT TRUE,
    last_triggered_at TIMESTAMPTZ    NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by        UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by        UUID           NOT NULL REFERENCES ces_service.users(id)
);
