-- V16__create_warehouse_tables.sql
-- Purpose: M19 — category_nodes (self-referencing tree), spare_parts, stock_entries,
-- spare_part_usages, stock_adjustments + indexes + stock triggers (SRS M19.1).
-- Also adds the deferred FK constraints from V11 (wo_cost_items.spare_part_id and
-- wo_cost_items.spare_part_usage_id) now that the referenced tables exist.

-- ─────────────────────────────────────────────────────────────────────────────
-- category_nodes  (self-referencing tree)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.category_nodes (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    parent_id    UUID          NULL REFERENCES ces_service.category_nodes(id),  -- NULL = root
    name         VARCHAR(255)  NOT NULL,
    slug         VARCHAR(255)  NOT NULL,
    depth        INTEGER       NOT NULL DEFAULT 0,
    path         TEXT          NOT NULL,
    sort_order   INTEGER       NOT NULL DEFAULT 0,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ   NULL,
    created_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    UNIQUE (branch_id, parent_id, slug)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- spare_parts
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.spare_parts (
    id                  UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id           UUID           NOT NULL REFERENCES ces_service.branches(id),
    category_node_id    UUID           NOT NULL REFERENCES ces_service.category_nodes(id),
    name                VARCHAR(255)   NOT NULL,
    sku                 VARCHAR(100)   NULL,
    unit                VARCHAR(50)    NOT NULL
                        CHECK (unit IN ('PIECE','LITER','KG','METER','PACKAGE')),
    current_stock       NUMERIC(10,3)  NOT NULL DEFAULT 0,
    minimum_stock       NUMERIC(10,3)  NOT NULL DEFAULT 0,
    unit_cost           NUMERIC(15,2)  NOT NULL DEFAULT 0,   -- last purchase cost
    default_margin_pct  NUMERIC(5,2)   NOT NULL DEFAULT 0,
    unit_sell_price     NUMERIC(15,2)  NOT NULL DEFAULT 0,   -- cost * (1 + margin)
    supplier_name       VARCHAR(255)   NULL,
    last_purchased_at   DATE           NULL,
    notes               TEXT           NULL,
    is_active           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ    NULL,
    created_by          UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by          UUID           NOT NULL REFERENCES ces_service.users(id)
);

CREATE INDEX idx_spare_parts_branch_cat
    ON ces_service.spare_parts(branch_id, category_node_id)
    WHERE deleted_at IS NULL AND is_active = TRUE;

CREATE INDEX idx_spare_parts_low_stock
    ON ces_service.spare_parts(branch_id)
    WHERE current_stock <= minimum_stock AND deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- stock_entries
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.stock_entries (
    id             UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id      UUID           NOT NULL REFERENCES ces_service.branches(id),
    spare_part_id  UUID           NOT NULL REFERENCES ces_service.spare_parts(id),
    quantity       NUMERIC(10,3)  NOT NULL,
    unit_cost      NUMERIC(15,2)  NOT NULL,
    total_cost     NUMERIC(15,2)  NOT NULL,
    supplier_name  VARCHAR(255)   NULL,
    invoice_number VARCHAR(100)   NULL,
    entry_date     DATE           NOT NULL DEFAULT CURRENT_DATE,
    notes          TEXT           NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by     UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_by     UUID           NOT NULL REFERENCES ces_service.users(id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- spare_part_usages  (stock-out, linked to a Work Order)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.spare_part_usages (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id       UUID           NOT NULL REFERENCES ces_service.branches(id),
    spare_part_id   UUID           NOT NULL REFERENCES ces_service.spare_parts(id),
    wo_id           UUID           NOT NULL REFERENCES ces_service.work_orders(id),
    wo_cost_item_id UUID           NULL REFERENCES ces_service.wo_cost_items(id),
    quantity        NUMERIC(10,3)  NOT NULL,
    unit_cost       NUMERIC(15,2)  NOT NULL,     -- cost at usage time (preserved)
    unit_sell_price NUMERIC(15,2)  NOT NULL,     -- sell price at usage time
    margin_pct      NUMERIC(5,2)   NOT NULL,
    total_cost      NUMERIC(15,2)  NOT NULL,
    total_sell      NUMERIC(15,2)  NOT NULL,
    usage_date      DATE           NOT NULL DEFAULT CURRENT_DATE,
    notes           TEXT           NULL,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by      UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_by      UUID           NOT NULL REFERENCES ces_service.users(id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- stock_adjustments
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.stock_adjustments (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id       UUID           NOT NULL REFERENCES ces_service.branches(id),
    spare_part_id   UUID           NOT NULL REFERENCES ces_service.spare_parts(id),
    quantity_before NUMERIC(10,3)  NOT NULL,
    quantity_after  NUMERIC(10,3)  NOT NULL,
    delta           NUMERIC(10,3)  NOT NULL,   -- quantity_after - quantity_before
    reason          TEXT           NOT NULL,
    adjustment_date DATE           NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by      UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_by      UUID           NOT NULL REFERENCES ces_service.users(id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Deferred FK constraints from V11 (wo_cost_items -> warehouse tables)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE ces_service.wo_cost_items
    ADD CONSTRAINT fk_wo_cost_items_spare_part
        FOREIGN KEY (spare_part_id) REFERENCES ces_service.spare_parts(id),
    ADD CONSTRAINT fk_wo_cost_items_spare_part_usage
        FOREIGN KEY (spare_part_usage_id) REFERENCES ces_service.spare_part_usages(id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Stock triggers (SRS M19.1)
-- ─────────────────────────────────────────────────────────────────────────────

-- stock_entries INSERT -> increase current_stock, update last cost & sell price
CREATE OR REPLACE FUNCTION ces_service.update_stock_on_entry()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE ces_service.spare_parts
    SET    current_stock     = current_stock + NEW.quantity,
           unit_cost         = NEW.unit_cost,
           unit_sell_price   = NEW.unit_cost * (1 + default_margin_pct / 100),
           last_purchased_at = NEW.entry_date
    WHERE  id = NEW.spare_part_id;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_stock_entry
    AFTER INSERT ON ces_service.stock_entries
    FOR EACH ROW EXECUTE FUNCTION ces_service.update_stock_on_entry();

-- spare_part_usages INSERT -> verify and decrease current_stock
CREATE OR REPLACE FUNCTION ces_service.update_stock_on_usage()
RETURNS TRIGGER AS $$
BEGIN
    IF (SELECT current_stock FROM ces_service.spare_parts
        WHERE id = NEW.spare_part_id) < NEW.quantity THEN
        RAISE EXCEPTION 'STOCK_INSUFFICIENT';
    END IF;
    UPDATE ces_service.spare_parts
    SET    current_stock = current_stock - NEW.quantity
    WHERE  id = NEW.spare_part_id;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_stock_usage
    BEFORE INSERT ON ces_service.spare_part_usages
    FOR EACH ROW EXECUTE FUNCTION ces_service.update_stock_on_usage();

-- stock_adjustments INSERT -> set current_stock to the adjusted value (SRS M19.3)
CREATE OR REPLACE FUNCTION ces_service.update_stock_on_adjustment()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE ces_service.spare_parts
    SET    current_stock = NEW.quantity_after
    WHERE  id = NEW.spare_part_id;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_stock_adjustment
    AFTER INSERT ON ces_service.stock_adjustments
    FOR EACH ROW EXECUTE FUNCTION ces_service.update_stock_on_adjustment();
