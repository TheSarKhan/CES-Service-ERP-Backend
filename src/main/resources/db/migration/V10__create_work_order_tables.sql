-- V10__create_work_order_tables.sql
-- Purpose: M06 — work_orders, work_order_status_history, work_order_comments,
-- work_order_attachments (SRS M06.1). The wo-number sequence + trigger are created in V23.
-- Archive columns and the auto-archive trigger are added later in V17.

CREATE TABLE ces_service.work_orders (
    id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id           UUID           NOT NULL REFERENCES ces_service.branches(id),
    wo_number           VARCHAR(50)    NOT NULL UNIQUE,   -- WO-2026-0001 (filled by V23 trigger)
    service_request_id  UUID           NOT NULL REFERENCES ces_service.service_requests(id),
    wo_type             VARCHAR(50)    NOT NULL
                        CHECK (wo_type IN ('PAID_SERVICE','COMPANY','INTERMEDIARY','INSPECTION')),
    status              VARCHAR(50)    NOT NULL DEFAULT 'NEW'
                        CHECK (status IN ('NEW','ASSIGNED','IN_PROGRESS','ON_HOLD','COMPLETED','CLOSED','CANCELLED')),
    priority            VARCHAR(20)    NOT NULL DEFAULT 'NORMAL'
                        CHECK (priority IN ('NORMAL','URGENT','CRITICAL')),
    assigned_to         UUID           NULL REFERENCES ces_service.users(id),
    assigned_at         TIMESTAMPTZ    NULL,
    started_at          TIMESTAMPTZ    NULL,
    completed_at        TIMESTAMPTZ    NULL,
    closed_at           TIMESTAMPTZ    NULL,
    closed_by           UUID           NULL REFERENCES ces_service.users(id),
    total_cost          NUMERIC(15,2)  NULL,
    total_sell          NUMERIC(15,2)  NULL,
    is_cost_approved    BOOLEAN        NOT NULL DEFAULT FALSE,
    cost_approved_by    UUID           NULL REFERENCES ces_service.users(id),
    cost_approved_at    TIMESTAMPTZ    NULL,
    closure_notes       TEXT           NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ    NULL,
    created_by          UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by          UUID           NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.work_order_status_history (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id     UUID          NOT NULL REFERENCES ces_service.branches(id),
    wo_id         UUID          NOT NULL REFERENCES ces_service.work_orders(id) ON DELETE CASCADE,
    from_status   VARCHAR(50)   NULL,    -- NULL = initial status
    to_status     VARCHAR(50)   NOT NULL,
    changed_by    UUID          NOT NULL REFERENCES ces_service.users(id),
    changed_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    notes         TEXT          NULL
);

CREATE TABLE ces_service.work_order_comments (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    wo_id        UUID          NOT NULL REFERENCES ces_service.work_orders(id) ON DELETE CASCADE,
    comment      TEXT          NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by   UUID          NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.work_order_attachments (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    wo_id        UUID          NOT NULL REFERENCES ces_service.work_orders(id) ON DELETE CASCADE,
    file_name    VARCHAR(255)  NOT NULL,
    file_url     TEXT          NOT NULL,
    file_type    VARCHAR(100)  NULL,
    file_size    BIGINT        NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by   UUID          NOT NULL REFERENCES ces_service.users(id)
);
