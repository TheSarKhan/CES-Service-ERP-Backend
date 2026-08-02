-- V27__create_inventory_tables.sql
-- Purpose: Generic dynamic Inventory Management module (Stok İdarəetməsi).
-- Independent of the existing V16 spare_parts/category_nodes tables (untouched —
-- those stay wired to Work Order costing). New tables:
--   inventory_nodes          — self-referencing physical storage tree (adjacency list,
--                              modelled on the Arxiv project's Folder pattern: parent_id
--                              only, no persisted depth/path, unbounded depth)
--   inventory_categories     — product category (Elektronika / Mebel / Kimya ...)
--   inventory_category_fields — dynamic per-category field definitions (EAV schema)
--   inventory_items          — products; dynamic attribute values live in a JSONB column
--   inventory_item_units     — individually serialized, warranty-tracked physical units

-- ─────────────────────────────────────────────────────────────────────────────
-- inventory_nodes  (dynamic physical Layer tree — adjacency list)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.inventory_nodes (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id   UUID          NOT NULL REFERENCES ces_service.branches(id),
    parent_id   UUID          NULL REFERENCES ces_service.inventory_nodes(id) ON DELETE CASCADE, -- NULL = root
    name        VARCHAR(255)  NOT NULL,
    code        VARCHAR(100)  NULL,
    qr_code     VARCHAR(255)  NULL,
    barcode     VARCHAR(255)  NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    notes       TEXT          NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ   NULL,
    created_by  UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by  UUID          NOT NULL REFERENCES ces_service.users(id),
    UNIQUE (branch_id, parent_id, name)
);

CREATE INDEX idx_inventory_nodes_parent ON ces_service.inventory_nodes(parent_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_inventory_nodes_qr ON ces_service.inventory_nodes(qr_code) WHERE qr_code IS NOT NULL;
CREATE UNIQUE INDEX uq_inventory_nodes_barcode ON ces_service.inventory_nodes(barcode) WHERE barcode IS NOT NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- inventory_categories
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.inventory_categories (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id     UUID          NOT NULL REFERENCES ces_service.branches(id),
    name          VARCHAR(255)  NOT NULL,
    default_unit  VARCHAR(50)   NOT NULL,
    is_active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ   NULL,
    created_by    UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by    UUID          NOT NULL REFERENCES ces_service.users(id),
    UNIQUE (branch_id, name)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- inventory_category_fields  (dynamic field definitions — EAV schema)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.inventory_category_fields (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id      UUID          NOT NULL REFERENCES ces_service.inventory_categories(id) ON DELETE CASCADE,
    field_key        VARCHAR(100)  NOT NULL,
    label            VARCHAR(255)  NOT NULL,
    field_type       VARCHAR(30)   NOT NULL
                      CHECK (field_type IN ('TEXT','NUMBER','DATE','CHECKBOX','SELECT','MULTI_SELECT','FILE','IMAGE','LINK')),
    options          JSONB         NULL,          -- SELECT / MULTI_SELECT choice list
    is_required      BOOLEAN       NOT NULL DEFAULT FALSE,
    default_value    TEXT          NULL,
    placeholder       VARCHAR(255) NULL,
    validation_regex VARCHAR(500)  NULL,
    sort_order       INTEGER       NOT NULL DEFAULT 0,
    is_visible       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (category_id, field_key)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- inventory_items  (products)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.inventory_items (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id       UUID           NOT NULL REFERENCES ces_service.branches(id),
    node_id         UUID           NOT NULL REFERENCES ces_service.inventory_nodes(id),
    category_id     UUID           NOT NULL REFERENCES ces_service.inventory_categories(id),
    name            VARCHAR(255)   NOT NULL,
    sku             VARCHAR(100)   NOT NULL,
    barcode         VARCHAR(255)   NULL,
    qr_code         VARCHAR(255)   NULL,
    unit            VARCHAR(50)    NOT NULL,
    quantity        NUMERIC(12,3)  NOT NULL DEFAULT 0,
    purchase_price  NUMERIC(15,2)  NOT NULL DEFAULT 0,
    is_serialized   BOOLEAN        NOT NULL DEFAULT FALSE, -- TRUE => stock tracked via inventory_item_units
    attributes      JSONB          NOT NULL DEFAULT '{}',  -- dynamic field values, keyed by field_key
    notes           TEXT           NULL,
    is_active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ    NULL,
    created_by      UUID           NOT NULL REFERENCES ces_service.users(id),
    updated_by      UUID           NOT NULL REFERENCES ces_service.users(id),
    UNIQUE (branch_id, sku)
);

CREATE INDEX idx_inventory_items_node ON ces_service.inventory_items(node_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_items_category ON ces_service.inventory_items(branch_id, category_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_inventory_items_low_stock ON ces_service.inventory_items(branch_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_inventory_items_qr ON ces_service.inventory_items(qr_code) WHERE qr_code IS NOT NULL;
CREATE UNIQUE INDEX uq_inventory_items_barcode ON ces_service.inventory_items(barcode) WHERE barcode IS NOT NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- inventory_item_units  (individually serialized, warranty-tracked units)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.inventory_item_units (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id             UUID          NOT NULL REFERENCES ces_service.branches(id),
    item_id               UUID          NOT NULL REFERENCES ces_service.inventory_items(id),
    node_id               UUID          NOT NULL REFERENCES ces_service.inventory_nodes(id),
    serial_number         VARCHAR(255)  NOT NULL,
    qr_code               VARCHAR(255)  NULL,
    barcode               VARCHAR(255)  NULL,
    status                VARCHAR(30)   NOT NULL DEFAULT 'IN_STOCK'
                          CHECK (status IN ('IN_STOCK','IN_USE','FAILED','DISPOSED','RETURNED')),
    purchase_date         DATE          NOT NULL DEFAULT CURRENT_DATE,
    warranty_start_date   DATE          NULL,
    warranty_end_date     DATE          NULL,
    failed_at             TIMESTAMPTZ   NULL,
    failure_notes         TEXT          NULL,
    used_in_work_order_id UUID          NULL REFERENCES ces_service.work_orders(id),
    notes                 TEXT          NULL,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMPTZ   NULL,
    created_by            UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by            UUID          NOT NULL REFERENCES ces_service.users(id),
    UNIQUE (branch_id, serial_number)
);

CREATE INDEX idx_item_units_item ON ces_service.inventory_item_units(item_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_item_units_warranty_end ON ces_service.inventory_item_units(branch_id, warranty_end_date) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_item_units_qr ON ces_service.inventory_item_units(qr_code) WHERE qr_code IS NOT NULL;
CREATE UNIQUE INDEX uq_item_units_barcode ON ces_service.inventory_item_units(barcode) WHERE barcode IS NOT NULL;
