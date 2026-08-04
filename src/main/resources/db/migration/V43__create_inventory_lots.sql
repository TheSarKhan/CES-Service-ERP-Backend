-- V43__create_inventory_lots.sql
-- Partiya (lot) və son istifadə tarixi.
--
-- Eyni məhsul müxtəlif vaxtlarda alınır və hər partiyanın öz bitmə tarixi olur. Məhsulda tək bir
-- tarix saxlamaq onları ayırd etməyə imkan vermir — köhnə partiya rəfin dibində qalıb bitir,
-- yenisi isə istifadə olunur.
--
-- Lot izlənməsi məhsulun BAYRAĞIDIR və seriyalı olmaqla eyni anda seçilə bilməz: məhsul ya
-- seriyalıdır (hər ədədin öz nömrəsi), ya lotludur (partiya-partiya), ya da adi. Üçü də eyni
-- vaxtda olsa, qalığın həqiqəti üç yerdə olardı.

ALTER TABLE ces_service.inventory_items
    ADD COLUMN is_lot_tracked BOOLEAN NOT NULL DEFAULT FALSE,
    -- Bitməsinə neçə gün qalmış xəbərdarlıq verilsin. Boşdursa zəmanətdəki kimi 30 gün işləyir.
    ADD COLUMN expiry_warning_days INTEGER;

ALTER TABLE ces_service.inventory_items
    ADD CONSTRAINT inventory_items_tracking_mode_chk
        CHECK (NOT (is_serialized AND is_lot_tracked));

COMMENT ON COLUMN ces_service.inventory_items.is_lot_tracked IS
    'Partiya izlənir. is_serialized ilə eyni anda ola bilməz — qalığın bir mənbəyi olmalıdır.';

CREATE TABLE ces_service.inventory_lots (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id     UUID          NOT NULL REFERENCES ces_service.branches(id),
    item_id       UUID          NOT NULL REFERENCES ces_service.inventory_items(id),
    node_id       UUID          NOT NULL REFERENCES ces_service.inventory_nodes(id),

    lot_number    VARCHAR(100)  NOT NULL,
    quantity      NUMERIC(12,3) NOT NULL DEFAULT 0,
    expiry_date   DATE,
    received_date DATE          NOT NULL DEFAULT CURRENT_DATE,
    notes         TEXT,

    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ,
    created_by    UUID,
    updated_by    UUID,

    CONSTRAINT inventory_lots_quantity_chk CHECK (quantity >= 0)
);

-- Eyni partiya eyni qovluqda iki sətir olmamalıdır — çıxış edəndə hansından götürüldüyü
-- qeyri-müəyyən olar.
CREATE UNIQUE INDEX ux_inventory_lots_item_node_number
    ON ces_service.inventory_lots (item_id, node_id, lot_number)
    WHERE deleted_at IS NULL;

-- FEFO: çıxışda ən tez bitən partiya təklif olunur. Tarixi olmayanlar sona düşür.
CREATE INDEX ix_inventory_lots_fefo
    ON ces_service.inventory_lots (item_id, node_id, expiry_date)
    WHERE deleted_at IS NULL AND quantity > 0;

-- «Bitmək üzrə partiyalar» sorğusu.
CREATE INDEX ix_inventory_lots_expiry
    ON ces_service.inventory_lots (branch_id, expiry_date)
    WHERE deleted_at IS NULL AND quantity > 0 AND expiry_date IS NOT NULL;

COMMENT ON TABLE ces_service.inventory_lots IS
    'Partiya qalığı. Lot izlənən məhsulda qovluq üzrə qalıq bu sətirlərin cəminə bərabər olmalıdır.';
