-- V11__create_cost_tables.sql
-- Purpose: M07 — wo_cost_items, margin_config (SRS M07.1).
--
-- Forward-reference note: wo_cost_items.spare_part_id and wo_cost_items.spare_part_usage_id
-- reference spare_parts / spare_part_usages, which are not created until V16. They are
-- declared here as plain nullable UUID columns WITHOUT FK constraints; the FK constraints are
-- added via ALTER TABLE in V16 once those warehouse tables exist.

CREATE TABLE ces_service.wo_cost_items (
    id                   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id            UUID           NOT NULL REFERENCES ces_service.branches(id),
    wo_id                UUID           NOT NULL REFERENCES ces_service.work_orders(id) ON DELETE CASCADE,
    item_type            VARCHAR(50)    NOT NULL
                         CHECK (item_type IN ('LABOR','PART','EXTERNAL_SERVICE','OTHER')),
    -- PART warehouse links (FKs added in V16):
    spare_part_id        UUID           NULL,
    spare_part_usage_id  UUID           NULL,
    description          VARCHAR(255)   NOT NULL,
    quantity             NUMERIC(10,3)  NOT NULL DEFAULT 1,
    unit_cost            NUMERIC(15,2)  NOT NULL,    -- cost (maya dəyəri)
    margin_percent       NUMERIC(5,2)   NOT NULL DEFAULT 0,
    unit_sell_price      NUMERIC(15,2)  NOT NULL,    -- unit_cost * (1 + margin/100)
    total_cost           NUMERIC(15,2)  NOT NULL,    -- quantity * unit_cost
    total_sell_price     NUMERIC(15,2)  NOT NULL,    -- quantity * unit_sell_price
    is_margin_overridden BOOLEAN        NOT NULL DEFAULT FALSE,
    margin_override_by   UUID           NULL REFERENCES ces_service.users(id),
    margin_override_at   TIMESTAMPTZ    NULL,
    notes                TEXT           NULL,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by           UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by           UUID           NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.margin_config (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id      UUID          NOT NULL REFERENCES ces_service.branches(id),
    item_type      VARCHAR(50)   NOT NULL,   -- LABOR | PART | EXTERNAL_SERVICE
    default_margin NUMERIC(5,2)  NOT NULL DEFAULT 0,
    min_margin     NUMERIC(5,2)  NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by     UUID          NOT NULL REFERENCES ces_service.users(id),
    UNIQUE (branch_id, item_type)
);
