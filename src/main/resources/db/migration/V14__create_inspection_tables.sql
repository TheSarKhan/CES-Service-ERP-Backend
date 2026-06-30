-- V14__create_inspection_tables.sql
-- Purpose: M18 — inspection_schedules, inspection_checklists, inspection_checklist_items (JSONB
-- options), inspections (generated_wo_id FK), inspection_results (SRS M18.1).
-- The ins-number sequence + trigger are created in V23. inspection_number stays NOT NULL UNIQUE
-- (safe: no migration rows; application inserts go through the V23 trigger).

CREATE TABLE ces_service.inspection_schedules (
    id                    UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id             UUID           NOT NULL REFERENCES ces_service.branches(id),
    vehicle_id            UUID           NOT NULL REFERENCES ces_service.vehicles(id),
    trigger_type          VARCHAR(50)    NOT NULL
                          CHECK (trigger_type IN ('HOURS','DAYS','BOTH')),
    hours_interval        NUMERIC(10,1)  NULL,
    days_interval         INTEGER        NULL,
    next_due_hours        NUMERIC(10,1)  NULL,
    next_due_date         DATE           NULL,
    warning_before_hours  NUMERIC(10,1)  NOT NULL DEFAULT 50,
    warning_before_days   INTEGER        NOT NULL DEFAULT 7,
    is_active             BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMPTZ    NULL,
    created_by            UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by            UUID           NOT NULL REFERENCES ces_service.users(id),
    UNIQUE (vehicle_id, branch_id)
);

CREATE TABLE ces_service.inspection_checklists (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    name         VARCHAR(255)  NOT NULL,
    description  TEXT          NULL,
    is_default   BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by   UUID          NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.inspection_checklist_items (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id        UUID          NOT NULL REFERENCES ces_service.branches(id),
    checklist_id     UUID          NOT NULL REFERENCES ces_service.inspection_checklists(id) ON DELETE CASCADE,
    item_name        VARCHAR(255)  NOT NULL,
    item_category    VARCHAR(100)  NULL,
    options          JSONB         NOT NULL,   -- ["Normal","Az","Çox","Çirkli"]
    requires_value   BOOLEAN       NOT NULL DEFAULT FALSE,
    value_unit       VARCHAR(50)   NULL,
    sort_order       INTEGER       NOT NULL DEFAULT 0,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by       UUID          NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.inspections (
    id                         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id                  UUID           NOT NULL REFERENCES ces_service.branches(id),
    inspection_number          VARCHAR(50)    NOT NULL UNIQUE,   -- INS-2026-0001 (V23 trigger)
    vehicle_id                 UUID           NOT NULL REFERENCES ces_service.vehicles(id),
    checklist_id               UUID           NOT NULL REFERENCES ces_service.inspection_checklists(id),
    inspector_id               UUID           NOT NULL REFERENCES ces_service.users(id),
    status                     VARCHAR(50)    NOT NULL DEFAULT 'IN_PROGRESS'
                               CHECK (status IN ('IN_PROGRESS','COMPLETED','CANCELLED')),
    overall_result             VARCHAR(50)    NULL
                               CHECK (overall_result IN ('OK','MINOR_ISSUE','CRITICAL_ISSUE')),
    engine_hours_at_inspection NUMERIC(10,1)  NULL,
    inspection_date            DATE           NOT NULL DEFAULT CURRENT_DATE,
    completed_at               TIMESTAMPTZ    NULL,
    notes                      TEXT           NULL,
    generated_wo_id            UUID           NULL REFERENCES ces_service.work_orders(id),
    created_at                 TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at                 TIMESTAMPTZ    NULL,
    created_by                 UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by                 UUID           NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.inspection_results (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id         UUID           NOT NULL REFERENCES ces_service.branches(id),
    inspection_id     UUID           NOT NULL REFERENCES ces_service.inspections(id) ON DELETE CASCADE,
    checklist_item_id UUID           NOT NULL REFERENCES ces_service.inspection_checklist_items(id),
    selected_option   VARCHAR(100)   NOT NULL,
    numeric_value     NUMERIC(10,2)  NULL,
    severity          VARCHAR(50)    NOT NULL DEFAULT 'OK'
                      CHECK (severity IN ('OK','WARNING','CRITICAL')),
    notes             TEXT           NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by        UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by        UUID           NOT NULL REFERENCES ces_service.users(id)
);
