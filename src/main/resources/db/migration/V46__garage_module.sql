-- V46__garage_module.sql
-- Qaraj modulu (M03) — vehicles/vehicle_documents V7-dən qalan skeleti brifə uyğun yenidən
-- formalaşdırır və yeni cədvəllər əlavə edir. Bax: backend/docs/qaraj-motosaat-plani.md.
--
-- Bu miqrasiya YALNIZ Qaraja aiddir. engine_hour_logs/engine_hour_alerts (Motosaat, Baxış)
-- toxunulmaz qalır — onlar növbəti miqrasiyanın işidir.

-- ─────────────────────────────────────────────────────────────────────────────
-- vehicles: sahiblik iki dəyərə düşür (investor fərqi gələcək Müştərilər moduluna saxlanılır)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE ces_service.vehicles
    DROP CONSTRAINT vehicles_garage_type_check;
ALTER TABLE ces_service.vehicles
    ADD CONSTRAINT vehicles_garage_type_check CHECK (garage_type IN ('COMPANY', 'CUSTOMER'));

-- customers cədvəli artıq mövcuddur (V8) — boş saxlamağın mənası yoxdur. NULL qalır ki,
-- COMPANY sahiblikdə sahib olmasın; CUSTOMER seçildikdə servis qatı məcburi edir.
ALTER TABLE ces_service.vehicles
    ADD CONSTRAINT vehicles_owner_customer_fkey FOREIGN KEY (owner_id)
        REFERENCES ces_service.customers(id);

-- status: CHECK-lə sərt siyahı yerinə garage_config_values(list_type='STATUS')-a qarşı servis
-- qatında yoxlanılır — admin yeni status əlavə edə bilməlidir, sabit enum bunu bacarmaz.
ALTER TABLE ces_service.vehicles
    DROP CONSTRAINT vehicles_status_check;
ALTER TABLE ces_service.vehicles
    ALTER COLUMN status SET DEFAULT 'Aktiv';

-- Kod (TEX-000001), ad, motosaat/KM istifadə bayraqları, KM keşi (Motosaat bunu dolduracaq).
ALTER TABLE ces_service.vehicles
    ADD COLUMN code               VARCHAR(20)    NULL,
    ADD COLUMN name                VARCHAR(255)   NULL,
    ADD COLUMN uses_engine_hours   BOOLEAN        NOT NULL DEFAULT TRUE,
    ADD COLUMN uses_km             BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN current_km          NUMERIC(12, 1) NULL DEFAULT 0,
    ADD COLUMN last_km_at          TIMESTAMPTZ    NULL;

-- Cədvəl boşdur (Java kodu heç vaxt yazmayıb) — NOT NULL/UNIQUE-ə köçmək data itkisi riski
-- daşımır.
ALTER TABLE ces_service.vehicles
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN name SET NOT NULL,
    ADD CONSTRAINT vehicles_code_key UNIQUE (code);

-- Şassi/seriya/qeydiyyat: identifikasiya nömrəsidir, daxil edildiyi halda unikal olmalıdır
-- (brif), bütün filiallar üzrə (SRS). Partial index-ə keçirilir ki, soft-silinmiş texnikanın
-- nömrəsi təkrar istifadə oluna bilsin — plain UNIQUE bunu bacarmır.
ALTER TABLE ces_service.vehicles
    DROP CONSTRAINT vehicles_chassis_number_key;
CREATE UNIQUE INDEX ux_vehicles_chassis_number ON ces_service.vehicles (chassis_number)
    WHERE chassis_number IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_vehicles_serial_number ON ces_service.vehicles (serial_number)
    WHERE serial_number IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_vehicles_plate_number ON ces_service.vehicles (plate_number)
    WHERE plate_number IS NOT NULL AND deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- Kod generasiyası: TEX-000001 — SR/WO/INS-dən fərqli olaraq ilsiz, brifin öz nümunəsinə görə.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE SEQUENCE ces_service.vehicle_code_seq START 1;

CREATE OR REPLACE FUNCTION ces_service.generate_vehicle_code()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.code IS NULL THEN
        NEW.code := 'TEX-' || LPAD(nextval('ces_service.vehicle_code_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_vehicle_code
    BEFORE INSERT ON ces_service.vehicles
    FOR EACH ROW EXECUTE FUNCTION ces_service.generate_vehicle_code();

-- ─────────────────────────────────────────────────────────────────────────────
-- vehicle_documents: brifin tələb etdiyi sənəd nömrəsi, verilmə tarixi, qeyd.
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE ces_service.vehicle_documents
    ADD COLUMN doc_number VARCHAR(100) NULL,
    ADD COLUMN issued_at  DATE         NULL,
    ADD COLUMN notes      TEXT         NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- vehicle_photos — kateqoriyalı fotolar.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.vehicle_photos (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    vehicle_id   UUID          NOT NULL REFERENCES ces_service.vehicles(id) ON DELETE CASCADE,
    category     VARCHAR(100)  NOT NULL,
    file_name    VARCHAR(255)  NOT NULL,
    file_url     TEXT          NOT NULL,
    file_size    BIGINT        NULL,
    notes        TEXT          NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at   TIMESTAMPTZ   NULL
);
CREATE INDEX idx_vehicle_photos_vehicle ON ces_service.vehicle_photos(vehicle_id)
    WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- vehicle_components — status idarə olunan tarixçə (InventoryItemUnit naxışı): ayrıca history
-- cədvəli yoxdur, komponent REMOVED olanda öz üstündə qalır, yenisi ACTIVE kimi yaradılır.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.vehicle_components (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id              UUID          NOT NULL REFERENCES ces_service.branches(id),
    vehicle_id             UUID          NOT NULL REFERENCES ces_service.vehicles(id) ON DELETE CASCADE,
    component_type         VARCHAR(100)  NOT NULL,
    identifier             VARCHAR(255)  NULL,
    status                 VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                           CHECK (status IN ('ACTIVE', 'REMOVED')),
    installed_at           DATE          NOT NULL DEFAULT CURRENT_DATE,
    installed_meter_value  NUMERIC(10, 1) NULL,
    removed_at             DATE          NULL,
    removed_meter_value    NUMERIC(10, 1) NULL,
    removal_reason         TEXT          NULL,
    notes                  TEXT          NULL,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by             UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by             UUID          NOT NULL REFERENCES ces_service.users(id),
    -- Distinct from status=REMOVED: that is a real swap (the part physically left the machine and
    -- the row records when). This is BaseEntity's usual escape hatch for "this row should never
    -- have existed" — a component logged against the wrong vehicle entirely, say.
    deleted_at             TIMESTAMPTZ   NULL
);
CREATE INDEX idx_vehicle_components_vehicle ON ces_service.vehicle_components(vehicle_id, status)
    WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- garage_config_values — bütün Qaraj açılan siyahıları TƏK cədvəldə. Anbarın EAV sistemi
-- (kateqoriya + dinamik sahə) burada lazım deyil — Qaraj heç bir tipə görə dinamik SAHƏ tələb
-- etmir, yalnız açılan siyahı dəyərləri.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.garage_config_values (
    id                         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id                  UUID          NOT NULL REFERENCES ces_service.branches(id),
    list_type                  VARCHAR(30)   NOT NULL
                               CHECK (list_type IN (
                                   'EQUIPMENT_TYPE', 'BRAND', 'MODEL', 'STATUS', 'LOCATION',
                                   'DOC_TYPE', 'PHOTO_CATEGORY', 'COMPONENT_TYPE',
                                   'MAINTENANCE_TYPE', 'METER_SOURCE'
                               )),
    value                      VARCHAR(255)  NOT NULL,
    -- DOC_TYPE / PHOTO_CATEGORY üçün: bu kateqoriya "əsas" sayılır (UI-da vurğulanır).
    -- Sərt DB-səviyyəli məcburiyyət deyil — brif texnikanın yaradılmasını bloklamağı tələb etmir.
    is_required                BOOLEAN       NOT NULL DEFAULT FALSE,
    -- Yalnız EQUIPMENT_TYPE sətirləri üçün mənalıdır: yeni texnika bu növdən yaradılanda
    -- vehicles.uses_engine_hours/uses_km bu dəyərlərdən başlanğıc götürür.
    default_uses_engine_hours  BOOLEAN       NULL,
    default_uses_km            BOOLEAN       NULL,
    -- STATUS və METER_SOURCE üçün: digər kod bu dəyərlərə ad ilə istinad edəcək, ona görə
    -- adi istifadəçi silə bilməməlidir (deaktiv edə bilər).
    is_system                  BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active                  BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order                 INTEGER       NOT NULL DEFAULT 0,
    created_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by                 UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at                 TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by                 UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at                 TIMESTAMPTZ   NULL
);
CREATE UNIQUE INDEX ux_garage_config_branch_type_value
    ON ces_service.garage_config_values(branch_id, list_type, value)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_garage_config_branch_type
    ON ces_service.garage_config_values(branch_id, list_type)
    WHERE deleted_at IS NULL AND is_active = TRUE;

-- ─────────────────────────────────────────────────────────────────────────────
-- Baxım şablonları (Konfiqurasiya hissəsi). Texnikanın öz fərdi planı və gecikmə hesablanması
-- Motosaat mərhələsinin işidir — bura yalnız "texnika növü üçün nümunə" tərifi daxildir.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.garage_maintenance_templates (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id      UUID          NOT NULL REFERENCES ces_service.branches(id),
    equipment_type VARCHAR(100)  NOT NULL,
    name           VARCHAR(255)  NOT NULL,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by     UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by     UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at     TIMESTAMPTZ   NULL
);

CREATE TABLE ces_service.garage_maintenance_template_items (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id              UUID          NOT NULL REFERENCES ces_service.branches(id),
    template_id            UUID          NOT NULL REFERENCES ces_service.garage_maintenance_templates(id) ON DELETE CASCADE,
    maintenance_type       VARCHAR(100)  NOT NULL,
    interval_meter_hours   NUMERIC(10, 1) NULL,
    interval_km            NUMERIC(10, 1) NULL,
    interval_calendar_days INTEGER       NULL,
    notes                  TEXT          NULL,
    sort_order             INTEGER       NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by             UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by             UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at             TIMESTAMPTZ   NULL,
    CONSTRAINT chk_template_item_has_interval CHECK (
        interval_meter_hours IS NOT NULL OR interval_km IS NOT NULL
        OR interval_calendar_days IS NOT NULL
    )
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Başlanğıc konfiqurasiya dəyərləri — brifin öz nümunələri. Bir filial üçün (Anbar demo
-- seedinin naxışı) — yeni filial əlavə edilərsə öz siyahısını özü qurar.
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
    v_branch UUID := '11111111-1111-1111-1111-111111111111';
    v_user   UUID := '00000000-0000-0000-0000-000000000000';
BEGIN
    IF NOT EXISTS (SELECT 1 FROM ces_service.branches WHERE id = v_branch) THEN
        RETURN;
    END IF;
    IF EXISTS (SELECT 1 FROM ces_service.garage_config_values
               WHERE branch_id = v_branch AND list_type = 'STATUS' AND deleted_at IS NULL) THEN
        RETURN;
    END IF;

    INSERT INTO ces_service.garage_config_values
        (branch_id, list_type, value, is_system, sort_order, created_by, updated_by)
    VALUES
        (v_branch, 'STATUS', 'Aktiv',              TRUE, 1, v_user, v_user),
        (v_branch, 'STATUS', 'Servisdə',            TRUE, 2, v_user, v_user),
        (v_branch, 'STATUS', 'Təmir gözləyir',      TRUE, 3, v_user, v_user),
        (v_branch, 'STATUS', 'Passiv',              TRUE, 4, v_user, v_user),
        (v_branch, 'STATUS', 'İstifadədən kənar',   TRUE, 5, v_user, v_user),
        (v_branch, 'STATUS', 'Satılıb',             TRUE, 6, v_user, v_user),
        (v_branch, 'STATUS', 'Arxiv',               TRUE, 7, v_user, v_user);

    INSERT INTO ces_service.garage_config_values
        (branch_id, list_type, value, default_uses_engine_hours, default_uses_km, sort_order, created_by, updated_by)
    VALUES
        (v_branch, 'EQUIPMENT_TYPE', 'Ekskavator', TRUE,  FALSE, 1, v_user, v_user),
        (v_branch, 'EQUIPMENT_TYPE', 'Buldozer',   TRUE,  FALSE, 2, v_user, v_user),
        (v_branch, 'EQUIPMENT_TYPE', 'Yükləyici',  TRUE,  FALSE, 3, v_user, v_user),
        (v_branch, 'EQUIPMENT_TYPE', 'Kran',       TRUE,  FALSE, 4, v_user, v_user),
        (v_branch, 'EQUIPMENT_TYPE', 'Forklift',   TRUE,  FALSE, 5, v_user, v_user),
        (v_branch, 'EQUIPMENT_TYPE', 'Generator',  TRUE,  FALSE, 6, v_user, v_user),
        (v_branch, 'EQUIPMENT_TYPE', 'Kompressor', TRUE,  FALSE, 7, v_user, v_user),
        (v_branch, 'EQUIPMENT_TYPE', 'Traktor',    TRUE,  FALSE, 8, v_user, v_user),
        (v_branch, 'EQUIPMENT_TYPE', 'Avtokran',   TRUE,  FALSE, 9, v_user, v_user);

    INSERT INTO ces_service.garage_config_values
        (branch_id, list_type, value, is_required, sort_order, created_by, updated_by)
    VALUES
        (v_branch, 'DOC_TYPE', 'Texniki pasport', TRUE,  1, v_user, v_user),
        (v_branch, 'DOC_TYPE', 'Sığorta',         FALSE, 2, v_user, v_user),
        (v_branch, 'DOC_TYPE', 'Texniki baxış',   FALSE, 3, v_user, v_user),
        (v_branch, 'DOC_TYPE', 'Alış sənədi',     FALSE, 4, v_user, v_user),
        (v_branch, 'DOC_TYPE', 'Gömrük sənədi',   FALSE, 5, v_user, v_user),
        (v_branch, 'DOC_TYPE', 'Zəmanət sənədi',  FALSE, 6, v_user, v_user),
        (v_branch, 'DOC_TYPE', 'Digər',           FALSE, 7, v_user, v_user);

    INSERT INTO ces_service.garage_config_values
        (branch_id, list_type, value, is_required, sort_order, created_by, updated_by)
    VALUES
        (v_branch, 'PHOTO_CATEGORY', 'Ön görünüş',      TRUE,  1, v_user, v_user),
        (v_branch, 'PHOTO_CATEGORY', 'Arxa görünüş',    TRUE,  2, v_user, v_user),
        (v_branch, 'PHOTO_CATEGORY', 'Sağ tərəf',       TRUE,  3, v_user, v_user),
        (v_branch, 'PHOTO_CATEGORY', 'Sol tərəf',       TRUE,  4, v_user, v_user),
        (v_branch, 'PHOTO_CATEGORY', 'Şassi nömrəsi',   TRUE,  5, v_user, v_user),
        (v_branch, 'PHOTO_CATEGORY', 'Mühərrik',        FALSE, 6, v_user, v_user),
        (v_branch, 'PHOTO_CATEGORY', 'Kabin',           FALSE, 7, v_user, v_user),
        (v_branch, 'PHOTO_CATEGORY', 'Avadanlıq',       FALSE, 8, v_user, v_user),
        (v_branch, 'PHOTO_CATEGORY', 'Digər',           FALSE, 9, v_user, v_user);

    INSERT INTO ces_service.garage_config_values
        (branch_id, list_type, value, sort_order, created_by, updated_by)
    VALUES
        (v_branch, 'COMPONENT_TYPE', 'Mühərrik',        1, v_user, v_user),
        (v_branch, 'COMPONENT_TYPE', 'Hidravlik nasos', 2, v_user, v_user),
        (v_branch, 'COMPONENT_TYPE', 'Akkumulyator',    3, v_user, v_user),
        (v_branch, 'COMPONENT_TYPE', 'Təkərlər',        4, v_user, v_user),
        (v_branch, 'COMPONENT_TYPE', 'Kovş',            5, v_user, v_user),
        (v_branch, 'COMPONENT_TYPE', 'Digər',           6, v_user, v_user);

    INSERT INTO ces_service.garage_config_values
        (branch_id, list_type, value, sort_order, created_by, updated_by)
    VALUES
        (v_branch, 'MAINTENANCE_TYPE', 'Yağ dəyişimi',                1, v_user, v_user),
        (v_branch, 'MAINTENANCE_TYPE', 'Yağ filtri dəyişimi',         2, v_user, v_user),
        (v_branch, 'MAINTENANCE_TYPE', 'Hava filtri dəyişimi',        3, v_user, v_user),
        (v_branch, 'MAINTENANCE_TYPE', 'Hidravlik sistem yoxlanışı',  4, v_user, v_user),
        (v_branch, 'MAINTENANCE_TYPE', 'Mühərrik yoxlanışı',          5, v_user, v_user),
        (v_branch, 'MAINTENANCE_TYPE', 'Təkər yoxlanışı',             6, v_user, v_user),
        (v_branch, 'MAINTENANCE_TYPE', 'Əyləc sistemi yoxlanışı',     7, v_user, v_user),
        (v_branch, 'MAINTENANCE_TYPE', 'Texniki baxış',               8, v_user, v_user);

    -- Digər kod (Motosaat) bu dəyərlərə ad ilə istinad edəcək (mənbə = "Servis"/"Baxım"),
    -- ona görə is_system = TRUE.
    INSERT INTO ces_service.garage_config_values
        (branch_id, list_type, value, is_system, sort_order, created_by, updated_by)
    VALUES
        (v_branch, 'METER_SOURCE', 'Manual', TRUE, 1, v_user, v_user),
        (v_branch, 'METER_SOURCE', 'Servis',  TRUE, 2, v_user, v_user),
        (v_branch, 'METER_SOURCE', 'Baxım',   TRUE, 3, v_user, v_user),
        (v_branch, 'METER_SOURCE', 'İdxal',   TRUE, 4, v_user, v_user),
        (v_branch, 'METER_SOURCE', 'Digər',   TRUE, 5, v_user, v_user);
END $$;
