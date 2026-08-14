-- V48__motosaat_maintenance_materials.sql
-- Links a maintenance completion's materials to real Anbar/Stok movements — the first cross-module
-- write from Motosaat into Inventory. Closes one of the "gələcəyə saxlanılanlar" items in
-- backend/docs/qaraj-motosaat-plani.md; decisions recorded there.
--
-- Two kinds of line, both optional, both repeatable per completion:
--   CONSUMABLE  — a quantity-tracked item (yağ, filtr). Submits a normal Anbar stock-out, which
--                 goes through Anbar's own existing approval queue exactly like any other
--                 stock-out — the completion itself is NOT held up waiting for it.
--   SERIALIZED  — a specific in-stock inventory unit (e.g. hidravlik nasos). Marked IN_USE
--                 directly (unit status changes are not approval-gated in Anbar) and mirrored as
--                 a new ACTIVE row in vehicle_components, so "Komponentlər" stays in sync without
--                 a separate manual step.
--
-- Item/unit/serial values are snapshotted at write time (inventory_item_name, unit, serial_number)
-- so this list keeps reading correctly even if the source Inventory row is later renamed or
-- soft-deleted.
CREATE TABLE ces_service.vehicle_maintenance_completion_materials (
    id                         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id                  UUID          NOT NULL REFERENCES ces_service.branches(id),
    completion_id              UUID          NOT NULL REFERENCES ces_service.vehicle_maintenance_completions(id),
    kind                       VARCHAR(20)   NOT NULL CHECK (kind IN ('CONSUMABLE', 'SERIALIZED')),

    -- CONSUMABLE
    inventory_item_id          UUID          NULL REFERENCES ces_service.inventory_items(id),
    inventory_item_name        VARCHAR(255)  NULL,
    inventory_node_id          UUID          NULL REFERENCES ces_service.inventory_nodes(id),
    quantity                   NUMERIC(12, 3) NULL,
    unit                       VARCHAR(50)   NULL,
    stock_approval_request_id  UUID          NULL REFERENCES ces_service.approval_requests(id),

    -- SERIALIZED
    inventory_unit_id          UUID          NULL REFERENCES ces_service.inventory_item_units(id),
    serial_number              VARCHAR(255)  NULL,
    vehicle_component_id       UUID          NULL REFERENCES ces_service.vehicle_components(id),

    notes                      TEXT          NULL,
    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by                 UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by                 UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at                 TIMESTAMPTZ   NULL,
    CONSTRAINT chk_material_kind_fields CHECK (
        (kind = 'CONSUMABLE' AND inventory_item_id IS NOT NULL AND inventory_node_id IS NOT NULL
             AND quantity IS NOT NULL AND inventory_unit_id IS NULL)
        OR
        (kind = 'SERIALIZED' AND inventory_unit_id IS NOT NULL AND inventory_item_id IS NULL
             AND inventory_node_id IS NULL AND quantity IS NULL)
    )
);
CREATE INDEX idx_vmcm_completion ON ces_service.vehicle_maintenance_completion_materials (completion_id)
    WHERE deleted_at IS NULL;
