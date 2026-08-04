-- V39__create_stock_movements.sql
-- Stok hərəkətlərinin jurnalı.
--
-- İndiyə qədər giriş/çıxış/sayım heç bir sətir buraxmırdı — yalnız audit jurnalına JSON kimi
-- düşürdü. Yəni «bu 50 ədəd haradan gəldi, kim gətirdi, o vaxt qalıq nə qədər idi?» sualının
-- cavabı yox idi.
--
-- Bu cədvəl həm məhsul kartındakı hərəkət tarixçəsinin, həm transferin, həm də inventarizasiya
-- fərqlərinin təməlidir. Sətirlər DƏYİŞDİRİLMİR və SİLİNMİR: səhv hərəkət düzəldilmir, əks
-- hərəkətlə bağlanır — mühasibat jurnalı kimi.

CREATE TABLE ces_service.inventory_stock_movements (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id      UUID          NOT NULL REFERENCES ces_service.branches(id),
    item_id        UUID          NOT NULL REFERENCES ces_service.inventory_items(id),
    node_id        UUID          NOT NULL REFERENCES ces_service.inventory_nodes(id),

    -- Seriyalı vahid hərəkət edibsə hansı olduğu; saylı məhsulda boş qalır.
    unit_id        UUID          NULL REFERENCES ces_service.inventory_item_units(id),

    movement_type  VARCHAR(20)   NOT NULL,

    -- İşarəli: giriş müsbət, çıxış mənfi. Cəmi həmişə qalığı verməlidir.
    quantity       NUMERIC(12,3) NOT NULL,
    -- Hərəkətdən SONRAKI qalıq. Saxlanılır ki, tarixçə sonrakı əməliyyatlardan asılı olmasın:
    -- sətirə baxan adam o anın vəziyyətini görür, yenidən hesablamağa ehtiyac qalmır.
    balance_after  NUMERIC(12,3) NOT NULL,

    -- Hərəkəti doğuran sənəd: təsdiq sorğusu, transfer, sayım, gələcəkdə iş sifarişi.
    reference_type VARCHAR(30)   NULL,
    reference_id   UUID          NULL,

    reason         TEXT          NULL,

    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by     UUID          NULL,
    -- BaseEntity ilə uyğunluq üçün; jurnal sətri dəyişmədiyi üçün praktikada toxunulmur.
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by     UUID          NULL,
    deleted_at     TIMESTAMPTZ   NULL,

    CONSTRAINT inventory_stock_movements_type_chk CHECK (movement_type IN (
        'IN',            -- stok girişi
        'OUT',           -- stok çıxışı
        'ADJUST',        -- sayım düzəlişi (fərq)
        'TRANSFER_OUT',  -- transferdə mənbədən çıxma
        'TRANSFER_IN',   -- transferdə təyinata gəlmə
        'UNIT_IN',       -- seriyalı vahidin qeydiyyatı
        'UNIT_OUT'       -- seriyalı vahidin silinməsi/çıxması
    )),
    -- Sıfır miqdarlı hərəkət heç nə demir, amma hesabatı çirkləndirir.
    CONSTRAINT inventory_stock_movements_quantity_chk CHECK (quantity <> 0)
);

-- Məhsul kartındakı tarixçə: bir məhsulun bütün hərəkətləri, ən yenisi əvvəldə.
CREATE INDEX ix_stock_movements_item
    ON ces_service.inventory_stock_movements (item_id, created_at DESC);

-- Qovluq üzrə hərəkət hesabatı.
CREATE INDEX ix_stock_movements_node
    ON ces_service.inventory_stock_movements (branch_id, node_id, created_at DESC);

-- «Bu transferin/sayımın hərəkətləri hansılardır?»
CREATE INDEX ix_stock_movements_reference
    ON ces_service.inventory_stock_movements (reference_type, reference_id)
    WHERE reference_id IS NOT NULL;

COMMENT ON TABLE ces_service.inventory_stock_movements IS
    'Dəyişməz stok hərəkət jurnalı. Hər qalıq dəyişikliyi burada bir sətirdir.';
