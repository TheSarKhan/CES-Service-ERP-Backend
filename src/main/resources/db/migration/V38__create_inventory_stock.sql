-- V38__create_inventory_stock.sql
-- Multi-location: eyni məhsul artıq bir neçə qovluqda saxlanıla bilər.
--
-- İndiyə qədər məhsulun özündə həm node_id, həm quantity vardı — yəni «HYD-40» YALNIZ bir rəfdə
-- ola bilərdi. Real anbarda isə eyni nasos həm Rəf A-da, həm Rəf C-də olur və «cəmi neçə var?»
-- sualının bir cavabı olmalıdır.
--
-- Ona görə məhsul KATALOQ qeydinə çevrilir (ad, SKU, kateqoriya, zəmanət, təchizatçı), qalıq isə
-- (məhsul, qovluq) cütü üzrə ayrıca cədvəldə saxlanılır. Məhsulun ümumi qalığı bu sətirlərin
-- cəmidir — iki yerdə saxlanılan rəqəm gec-tez bir-birini tutmur.

CREATE TABLE ces_service.inventory_stock (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id   UUID          NOT NULL REFERENCES ces_service.branches(id),
    item_id     UUID          NOT NULL REFERENCES ces_service.inventory_items(id),
    node_id     UUID          NOT NULL REFERENCES ces_service.inventory_nodes(id),
    quantity    NUMERIC(12,3) NOT NULL DEFAULT 0,

    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ   NULL,
    created_by  UUID          NULL,
    updated_by  UUID          NULL,

    CONSTRAINT inventory_stock_quantity_chk CHECK (quantity >= 0)
);

-- Bir məhsulun bir qovluqda yalnız bir qalıq sətri ola bilər — əks halda «cəmi» sualının
-- cavabı sətirlərin necə qruplaşdırılmasından asılı olardı.
CREATE UNIQUE INDEX ux_inventory_stock_item_node
    ON ces_service.inventory_stock (item_id, node_id)
    WHERE deleted_at IS NULL;

-- «Bu qovluqda hansı məhsullar var?» — anbar naviqasiyasının əsas sorğusu.
CREATE INDEX ix_inventory_stock_node
    ON ces_service.inventory_stock (branch_id, node_id)
    WHERE deleted_at IS NULL;

-- «Bu məhsul hansı yerlərdədir?» — məhsul kartındakı yerlər siyahısı.
CREATE INDEX ix_inventory_stock_item
    ON ces_service.inventory_stock (item_id)
    WHERE deleted_at IS NULL;

-- ── Mövcud məlumatın köçürülməsi ────────────────────────────────────────────
-- Seriyasız məhsul: öz node-una öz miqdarı ilə tək sətir.
INSERT INTO ces_service.inventory_stock (branch_id, item_id, node_id, quantity, created_by, updated_by)
SELECT i.branch_id, i.id, i.node_id, i.quantity, i.created_by, i.updated_by
FROM ces_service.inventory_items i
WHERE i.deleted_at IS NULL
  AND i.is_serialized = FALSE;

-- Seriyalı məhsul: həqiqət vahidlərdədir, ona görə qalıq vahidlərdən hesablanır. Vahidlər onsuz
-- da öz node_id-lərini daşıyır — yəni onlar bu gün də multi-location idi.
-- Silinmiş (DISPOSED) vahidlər fiziki olaraq anbarda yoxdur, saya girmir.
INSERT INTO ces_service.inventory_stock (branch_id, item_id, node_id, quantity, created_by, updated_by)
SELECT u.branch_id, u.item_id, u.node_id, COUNT(*), i.created_by, i.updated_by
FROM ces_service.inventory_item_units u
JOIN ces_service.inventory_items i ON i.id = u.item_id AND i.deleted_at IS NULL
WHERE u.deleted_at IS NULL
  AND u.status <> 'DISPOSED'
GROUP BY u.branch_id, u.item_id, u.node_id, i.created_by, i.updated_by;

-- Vahidi olmayan seriyalı məhsul da yerini itirməməlidir — sıfır qalıqlı sətir qalır ki, məhsul
-- öz qovluğunda görünsün və ora vahid qeyd edilə bilsin.
INSERT INTO ces_service.inventory_stock (branch_id, item_id, node_id, quantity, created_by, updated_by)
SELECT i.branch_id, i.id, i.node_id, 0, i.created_by, i.updated_by
FROM ces_service.inventory_items i
WHERE i.deleted_at IS NULL
  AND i.is_serialized = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM ces_service.inventory_stock s
      WHERE s.item_id = i.id AND s.node_id = i.node_id AND s.deleted_at IS NULL
  );

COMMENT ON TABLE ces_service.inventory_stock IS
    'Məhsulun qovluq üzrə qalığı. Məhsulun ümumi qalığı bu sətirlərin cəmidir.';

-- ── Köhnə sütunlar ──────────────────────────────────────────────────────────
-- Silinmir, adı dəyişdirilir. Səbəb: bu, modulun ən böyük miqrasiyasıdır və köçürmədə bir şey
-- səhv gedərsə geri dönüş yolu qalmalıdır. Yumşaq silinmiş məhsullar yuxarıdakı köçürməyə
-- düşmür — onların yeri yalnız burada qalır.
--
-- Ad qəsdən «legacy_» ilə başlayır: heç bir kod ona müraciət etməməlidir, qalığın yeganə
-- mənbəyi inventory_stock-dur.
ALTER TABLE ces_service.inventory_items
    RENAME COLUMN node_id TO legacy_node_id;

ALTER TABLE ces_service.inventory_items
    RENAME COLUMN quantity TO legacy_quantity;

ALTER TABLE ces_service.inventory_items
    ALTER COLUMN legacy_node_id DROP NOT NULL,
    ALTER COLUMN legacy_quantity DROP NOT NULL;

COMMENT ON COLUMN ces_service.inventory_items.legacy_node_id IS
    'V38-dən əvvəlki tək yer. İstifadə edilmir — qalıq inventory_stock cədvəlindədir.';
COMMENT ON COLUMN ces_service.inventory_items.legacy_quantity IS
    'V38-dən əvvəlki qalıq. İstifadə edilmir — qalıq inventory_stock cədvəlindədir.';
