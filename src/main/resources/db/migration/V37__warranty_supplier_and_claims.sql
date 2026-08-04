-- V37__warranty_supplier_and_claims.sql
-- Zəmanət modulunun iki boşluğunu bağlayır:
--
--  1. TƏCHİZATÇI — zəmanət tələbi konkret təchizatçıya göndərilir, amma indiyə qədər bu məlumat
--     yalnız kateqoriyanın dinamik "istehsalci" sahəsində, JSONB içində saxlanılırdı. Oradan nə
--     süzmək, nə qruplaşdırmaq mümkün deyildi. İndi məhsulun öz sütunudur.
--
--  2. TƏLƏBİN NƏTİCƏSİ — "sıradan çıxdı" nasazlığı qeyd edirdi, amma tələbin göndərilib-
--     göndərilmədiyi, təchizatçının qəbul edib-etmədiyi heç yerdə yazılmırdı. Yəni modulun əsas
--     sualı — "bunu kim ödədi?" — sistemdə cavabsız qalırdı. warranty_claims bunu qeyd edir.

-- ── 1. Təchizatçı ───────────────────────────────────────────────────────────
ALTER TABLE ces_service.inventory_items
    ADD COLUMN supplier VARCHAR(255);

COMMENT ON COLUMN ces_service.inventory_items.supplier IS
    'Zəmanət tələbinin ünvanlandığı təchizatçı / istehsalçı.';

-- Mövcud məhsullarda bu məlumat artıq "istehsalci" sistem sahəsində ola bilər — köçürürük ki,
-- süzgəc ilk gündən boş olmasın. Boş sətirlər NULL qalır.
-- jsonb_exists(...) yerinə `?` operatoru işlədilmir: JDBC sürücüsü onu parametr kimi oxuya bilər.
UPDATE ces_service.inventory_items
SET supplier = NULLIF(TRIM(attributes ->> 'istehsalci'), '')
WHERE jsonb_exists(attributes, 'istehsalci');

CREATE INDEX ix_inventory_items_supplier
    ON ces_service.inventory_items (branch_id, supplier)
    WHERE deleted_at IS NULL AND supplier IS NOT NULL;

-- Zəmanət axtarışı barkod və QR üzrə də axtarır — etiketdə məhz onlar var.
CREATE INDEX ix_inventory_item_units_barcode_lookup
    ON ces_service.inventory_item_units (branch_id, barcode)
    WHERE deleted_at IS NULL AND barcode IS NOT NULL;

-- ── 2. Zəmanət tələbləri ────────────────────────────────────────────────────
-- Bir sətir = təchizatçıya göndərilmiş bir tələb. Hədəf ya konkret seriyalı vahid, ya da bütöv
-- (seriyasız) məhsuldur — uzatma tarixçəsi ilə eyni target_type/target_id sxemi.
--
-- status axını:  SUBMITTED → ACCEPTED | REJECTED → (RESOLVED)
--   ACCEPTED = təchizatçı zəmanəti tanıdı, xərc onun üzərinədir
--   REJECTED = tanımadı, xərc bizim üzərimizdədir
--   RESOLVED = iş faktiki bağlandı (əvəzləndi / təmir edildi / məbləğ qaytarıldı)
CREATE TABLE ces_service.warranty_claims (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id          UUID         NOT NULL,

    -- INVENTORY_ITEM | INVENTORY_ITEM_UNIT
    target_type        VARCHAR(30)  NOT NULL,
    target_id          UUID         NOT NULL,
    target_label       VARCHAR(255),
    -- Vahid üzrə tələbdə də məhsula qayıtmaq üçün — naviqasiya və hesabat üçün lazımdır.
    item_id            UUID,

    supplier           VARCHAR(255),
    -- Təchizatçının verdiyi istinad nömrəsi (varsa) — yazışmanı tapmaq üçün.
    claim_number       VARCHAR(100),

    status             VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED',
    resolution         VARCHAR(20),

    description        TEXT,
    decision_notes     TEXT,

    submitted_at       DATE         NOT NULL DEFAULT CURRENT_DATE,
    decided_at         DATE,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at         TIMESTAMPTZ,
    created_by         UUID,
    updated_by         UUID,

    CONSTRAINT warranty_claims_target_type_chk
        CHECK (target_type IN ('INVENTORY_ITEM', 'INVENTORY_ITEM_UNIT')),
    CONSTRAINT warranty_claims_status_chk
        CHECK (status IN ('SUBMITTED', 'ACCEPTED', 'REJECTED', 'RESOLVED')),
    CONSTRAINT warranty_claims_resolution_chk
        CHECK (resolution IS NULL OR resolution IN ('REPLACED', 'REPAIRED', 'REFUNDED', 'NONE'))
);

-- Axtarış sətirlərinə "tələb açılıb?" nişanı yapışdırmaq üçün hədəf üzrə ən son tələb lazımdır.
CREATE INDEX ix_warranty_claims_target
    ON ces_service.warranty_claims (branch_id, target_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_warranty_claims_status
    ON ces_service.warranty_claims (branch_id, status, submitted_at DESC)
    WHERE deleted_at IS NULL;
