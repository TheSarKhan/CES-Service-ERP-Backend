-- V35__add_item_warranty.sql
-- Purpose: zəmanət indiyə qədər yalnız seriyalı vahidlərdə (inventory_item_units) izlənirdi.
-- İndi məhsulun özündə də zəmanət var:
--
--   * Adi (saylı) məhsul üçün — warranty_start_date / warranty_end_date faktiki zəmanət
--     pəncərəsidir: bir partiya bir zəmanətlə alınır.
--   * Seriyalı məhsul üçün — warranty_months DEFAULT müddətdir: yeni vahid qeydə alınanda
--     bitmə tarixi bundan hesablanır, sonra vahid üzrə ayrıca dəyişdirilə bilər.
--     Seriyalı məhsulun öz warranty_end_date-i doldurulmur, çünki həqiqət vahiddədir.
--
-- warranty_months hər iki halda saxlanılır ki, uzatma və avtomatik hesablama üçün istinad olsun.

ALTER TABLE ces_service.inventory_items
    ADD COLUMN warranty_months     INTEGER,
    ADD COLUMN warranty_start_date DATE,
    ADD COLUMN warranty_end_date   DATE;

COMMENT ON COLUMN ces_service.inventory_items.warranty_months IS
    'Zəmanət müddəti (ay). Seriyalı məhsulda yeni vahidlər üçün default kimi işləyir.';
COMMENT ON COLUMN ces_service.inventory_items.warranty_end_date IS
    'Yalnız seriyasız məhsul üçün doldurulur; seriyalıda zəmanət vahid səviyyəsindədir.';

-- «Bitmək üzrə» sorğuları bitmə tarixinə görə süzür — indeks yalnız zəmanəti olanları saxlayır.
CREATE INDEX ix_inventory_items_warranty_end
    ON ces_service.inventory_items (branch_id, warranty_end_date)
    WHERE deleted_at IS NULL AND warranty_end_date IS NOT NULL;

CREATE INDEX ix_inventory_item_units_warranty_end
    ON ces_service.inventory_item_units (branch_id, warranty_end_date)
    WHERE deleted_at IS NULL AND warranty_end_date IS NOT NULL;

-- ── Uzatma tarixçəsi ────────────────────────────────────────────────────────
-- Uzatma maliyyə təsirlidir, ona görə «kim, nə vaxt, hansı tarixdən hansına, niyə» sualı
-- həmişə cavablana bilməlidir. Sətir yalnız təsdiqdən SONRA yazılır — yəni burada olan
-- hər qeyd faktiki tətbiq olunmuş uzatmadır.
CREATE TABLE ces_service.warranty_extensions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id          UUID        NOT NULL,

    -- INVENTORY_ITEM | INVENTORY_ITEM_UNIT
    target_type        VARCHAR(30) NOT NULL,
    target_id          UUID        NOT NULL,
    target_label       VARCHAR(255),

    previous_end_date  DATE,
    new_end_date       DATE        NOT NULL,
    months_added       INTEGER,
    reason             TEXT,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ,
    created_by         UUID,
    updated_by         UUID,

    CONSTRAINT warranty_extensions_target_type_chk
        CHECK (target_type IN ('INVENTORY_ITEM', 'INVENTORY_ITEM_UNIT'))
);

CREATE INDEX ix_warranty_extensions_target
    ON ces_service.warranty_extensions (target_type, target_id, created_at DESC)
    WHERE deleted_at IS NULL;
