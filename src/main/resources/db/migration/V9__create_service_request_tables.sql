-- V9__create_service_request_tables.sql
-- Purpose: M05 — service_requests (intermediary_profit GENERATED), service_request_services,
-- service_request_tasks. The SR-number sequence + trigger are created later in V23 (per the
-- SRS migration ordering). request_number stays NOT NULL UNIQUE; that is safe because no rows
-- are inserted during migrations — application inserts always go through the V23 trigger.

CREATE TABLE ces_service.service_requests (
    id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id           UUID           NOT NULL REFERENCES ces_service.branches(id),
    request_number      VARCHAR(50)    NOT NULL UNIQUE,   -- SR-2026-0001 (filled by V23 trigger)
    request_type        VARCHAR(50)    NOT NULL
                        CHECK (request_type IN ('PAID_SERVICE','COMPANY','INTERMEDIARY')),
    status              VARCHAR(50)    NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN','IN_PROGRESS','COMPLETED','CLOSED','CANCELLED')),
    priority            VARCHAR(20)    NOT NULL DEFAULT 'NORMAL'
                        CHECK (priority IN ('NORMAL','URGENT','CRITICAL')),
    customer_id         UUID           NULL REFERENCES ces_service.customers(id),
    vehicle_id          UUID           NOT NULL REFERENCES ces_service.vehicles(id),
    complaint           TEXT           NOT NULL,
    assigned_to         UUID           NULL REFERENCES ces_service.users(id),
    -- Intermediary (vasitəçilik) fields:
    contractor_id       UUID           NULL REFERENCES ces_service.contractors(id),
    customer_amount     NUMERIC(15,2)  NULL,   -- charged to customer
    contractor_amount   NUMERIC(15,2)  NULL,   -- paid to contractor
    intermediary_profit NUMERIC(15,2)
        GENERATED ALWAYS AS (customer_amount - contractor_amount) STORED,
    notes               TEXT           NULL,
    closed_at           TIMESTAMPTZ    NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ    NULL,
    created_by          UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by          UUID           NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.service_request_services (
    id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id           UUID           NOT NULL REFERENCES ces_service.branches(id),
    service_request_id  UUID           NOT NULL REFERENCES ces_service.service_requests(id) ON DELETE CASCADE,
    service_name        VARCHAR(255)   NOT NULL,
    pricing_type        VARCHAR(20)    NOT NULL
                        CHECK (pricing_type IN ('FIXED','HOURLY')),
    unit_price          NUMERIC(15,2)  NOT NULL,
    hours_worked        NUMERIC(5,2)   NULL,       -- HOURLY only
    total_price         NUMERIC(15,2)  NOT NULL,
    notes               TEXT           NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by          UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by          UUID           NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.service_request_tasks (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id           UUID          NOT NULL REFERENCES ces_service.branches(id),
    service_request_id  UUID          NOT NULL REFERENCES ces_service.service_requests(id) ON DELETE CASCADE,
    task_description    TEXT          NOT NULL,
    assigned_to         UUID          NULL REFERENCES ces_service.users(id),
    status              VARCHAR(50)   NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN','IN_PROGRESS','COMPLETED')),
    started_at          TIMESTAMPTZ   NULL,
    completed_at        TIMESTAMPTZ   NULL,
    notes               TEXT          NULL,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by          UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by          UUID          NOT NULL REFERENCES ces_service.users(id)
);
