-- V42__create_stocktakes.sql
-- İnventarizasiya (sayım) — KOR sayım.
--
-- Sayan adam sistemdəki miqdarı GÖRMÜR. Səbəb: açıq sayımda insan rəqəmi yoxlamaq əvəzinə
-- təsdiqləyir — «sistemdə 50 yazılıb, deməli 50 yazım». Onda sayımın heç bir mənası qalmır.
-- Fərq yalnız sayım bağlananda hesablanır.
--
-- Sistem miqdarı sətir yaradılanda (yəni sayım BAŞLAYANDA) dondurulur. Əks halda sayım gedərkən
-- başqa birinin etdiyi giriş fərqi dəyişdirər və rəqəm heç kimin saymadığı bir şeyə çevrilər.

CREATE TABLE ces_service.inventory_stocktakes (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id     UUID          NOT NULL REFERENCES ces_service.branches(id),

    -- Sayımın əhatəsi: bu qovluq (alt qovluqlar daxil deyil — rəf-rəf sayılır).
    node_id       UUID          NOT NULL REFERENCES ces_service.inventory_nodes(id),

    status        VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    notes         TEXT,

    -- Bağlananda yaradılan təsdiq sorğusu. Bütöv sayıma BİR təsdiq düşür: 200 sətri ayrı-ayrı
    -- təsdiqləmək praktikada təsdiqi mənasız formallığa çevirir.
    approval_request_id UUID,

    opened_by     UUID,
    opened_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    closed_by     UUID,
    closed_at     TIMESTAMPTZ,
    applied_at    TIMESTAMPTZ,

    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ,
    created_by    UUID,
    updated_by    UUID,

    CONSTRAINT inventory_stocktakes_status_chk
        CHECK (status IN ('OPEN', 'PENDING_APPROVAL', 'APPLIED', 'CANCELLED'))
);

-- Bir qovluqda eyni anda iki sayım getməməlidir — hansı rəqəmin tətbiq olunacağı qeyri-müəyyən
-- olar. Qismən unikal indeks yalnız açıq/təsdiq gözləyənlərə baxır.
CREATE UNIQUE INDEX ux_stocktakes_open_node
    ON ces_service.inventory_stocktakes (node_id)
    WHERE deleted_at IS NULL AND status IN ('OPEN', 'PENDING_APPROVAL');

CREATE INDEX ix_stocktakes_status
    ON ces_service.inventory_stocktakes (branch_id, status, opened_at DESC)
    WHERE deleted_at IS NULL;

-- ── Sətirlər ────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.inventory_stocktake_lines (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id         UUID          NOT NULL REFERENCES ces_service.branches(id),
    stocktake_id      UUID          NOT NULL REFERENCES ces_service.inventory_stocktakes(id) ON DELETE CASCADE,
    item_id           UUID          NOT NULL REFERENCES ces_service.inventory_items(id),

    -- Sayım başlayanda dondurulan sistem miqdarı. Sayan adam bunu GÖRMÜR.
    system_quantity   NUMERIC(12,3) NOT NULL,
    -- Faktiki sayılan. NULL = hələ sayılmayıb (sıfır sayılıbdan fərqlidir!).
    counted_quantity  NUMERIC(12,3),
    counted_by        UUID,
    counted_at        TIMESTAMPTZ,
    notes             TEXT,

    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID,
    updated_by        UUID,

    CONSTRAINT stocktake_lines_counted_chk CHECK (counted_quantity IS NULL OR counted_quantity >= 0)
);

CREATE UNIQUE INDEX ux_stocktake_lines_take_item
    ON ces_service.inventory_stocktake_lines (stocktake_id, item_id)
    WHERE deleted_at IS NULL;

COMMENT ON COLUMN ces_service.inventory_stocktake_lines.counted_quantity IS
    'NULL = sayılmayıb. Sıfır isə «yoxdur» deməkdir — ikisi eyni şey deyil.';
COMMENT ON COLUMN ces_service.inventory_stocktake_lines.system_quantity IS
    'Sayım başlayanda dondurulub; sayım gedərkən dəyişən qalıq fərqi korlamasın deyə.';
