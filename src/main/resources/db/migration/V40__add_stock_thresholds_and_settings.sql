-- V40__add_stock_thresholds_and_settings.sql
-- Minimum və kritik stok həddi + filial səviyyəli anbar tənzimləmələri.
--
-- Hədd MƏHSULUN özündədir və ÜMUMİ qalıqla müqayisə olunur — rəf üzrə deyil. Səbəb: «yenidən
-- sifariş verim?» sualı məhsul haqqındadır. Rəf A boşalsa da anbarda 40 ədəd varsa, sifariş
-- lazım deyil; rəf boşluğu köçürmə məsələsidir, satınalma məsələsi deyil.

ALTER TABLE ces_service.inventory_items
    ADD COLUMN min_quantity      NUMERIC(12,3),
    ADD COLUMN critical_quantity NUMERIC(12,3);

COMMENT ON COLUMN ces_service.inventory_items.min_quantity IS
    'Yenidən sifariş həddi. Ümumi qalıq bundan aşağı düşəndə xəbərdarlıq verilir.';
COMMENT ON COLUMN ces_service.inventory_items.critical_quantity IS
    'Kritik hədd — minimumdan aşağı olmalıdır; iş dayanma riski deməkdir.';

-- Hədd təyin edilməmiş məhsullar sorğuya heç girmir, ona görə qismən indeks.
CREATE INDEX ix_inventory_items_min_quantity
    ON ces_service.inventory_items (branch_id)
    WHERE deleted_at IS NULL AND min_quantity IS NOT NULL;

-- ── Anbar tənzimləmələri ────────────────────────────────────────────────────
-- Filial başına bir sətir. İki tənzimləmə var və hər ikisi istifadəçinin seçimidir:
--   * notification_emails — gündəlik xülasə kimə getsin (əl ilə siyahı)
--   * transfer_requires_different_receiver — transferi göndərəndən başqası qəbul etməlidirmi
--     (böyük filialda nəzarət, tək anbardarlı filialda blok olmasın)
CREATE TABLE ces_service.inventory_settings (
    id                                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id                             UUID        NOT NULL REFERENCES ces_service.branches(id),

    -- JSONB massiv: ["anbar@sirket.az", "satinalma@sirket.az"]
    notification_emails                   JSONB       NOT NULL DEFAULT '[]',
    daily_digest_enabled                  BOOLEAN     NOT NULL DEFAULT TRUE,
    transfer_requires_different_receiver  BOOLEAN     NOT NULL DEFAULT TRUE,

    created_at                            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at                            TIMESTAMPTZ,
    created_by                            UUID,
    updated_by                            UUID
);

CREATE UNIQUE INDEX ux_inventory_settings_branch
    ON ces_service.inventory_settings (branch_id)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE ces_service.inventory_settings IS
    'Filial üzrə anbar tənzimləmələri. Sətir yoxdursa default dəyərlər işləyir.';
