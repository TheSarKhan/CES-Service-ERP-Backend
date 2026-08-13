-- V47__motosaat_module.sql
-- Motosaat modulu (M08) — engine_hour_logs-u brifin ümumiləşdirilmiş "Meter Type" konsepsiyasına
-- uyğun meter_readings-ə çevirir və periodik baxım planını əlavə edir. Bax:
-- backend/docs/qaraj-motosaat-plani.md.
--
-- engine_hour_alerts (Baxış moduluna aiddir, bax plan sənədi) toxunulmaz qalır.

-- ─────────────────────────────────────────────────────────────────────────────
-- engine_hour_logs → meter_readings: yalnız motosaat deyil, KM də saxlaya bilsin
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE ces_service.engine_hour_logs RENAME TO meter_readings;
ALTER TABLE ces_service.meter_readings RENAME COLUMN hours_value TO value;

ALTER TABLE ces_service.meter_readings
    ADD COLUMN meter_type VARCHAR(20) NOT NULL DEFAULT 'ENGINE_HOURS'
        CHECK (meter_type IN ('ENGINE_HOURS', 'KILOMETERS'));
ALTER TABLE ces_service.meter_readings
    ALTER COLUMN meter_type DROP DEFAULT;

-- BaseEntity hər entity-dən deleted_at tələb edir (Qarajda tapılan eyni tələ, bax plan sənədi).
ALTER TABLE ces_service.meter_readings
    ADD COLUMN deleted_at TIMESTAMPTZ NULL;

-- entry_type sərt CHECK siyahısından çıxır, garage_config_values(METER_SOURCE)-a qarşı servis
-- qatında yoxlanılır — V46-da bu siyahı artıq is_system=TRUE olaraq səyahələnib (Manual/Servis/
-- Baxım/İdxal/Digər), digər kod (baxım tamamlanması) bu adlara istinad edəcək.
ALTER TABLE ces_service.meter_readings RENAME COLUMN entry_type TO source;
ALTER TABLE ces_service.meter_readings ALTER COLUMN source DROP DEFAULT;
ALTER TABLE ces_service.meter_readings ALTER COLUMN source TYPE VARCHAR(100);
ALTER TABLE ces_service.meter_readings DROP CONSTRAINT engine_hour_logs_entry_type_check;

DROP INDEX ces_service.idx_eh_logs_vehicle_date;
CREATE INDEX idx_meter_readings_vehicle_type_date
    ON ces_service.meter_readings (vehicle_id, meter_type, recorded_at DESC);

-- ─────────────────────────────────────────────────────────────────────────────
-- Sync trigger: meter_type-a görə vehicles-in hansı keş sütununu yeniləyəcəyini seçir
-- ─────────────────────────────────────────────────────────────────────────────
DROP TRIGGER trg_sync_engine_hours ON ces_service.meter_readings;
DROP FUNCTION ces_service.sync_vehicle_engine_hours();

CREATE OR REPLACE FUNCTION ces_service.sync_vehicle_meter()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.meter_type = 'ENGINE_HOURS' THEN
        UPDATE ces_service.vehicles
        SET    current_engine_hours = NEW.value,
               last_engine_hours_at = NEW.created_at
        WHERE  id = NEW.vehicle_id;
    ELSE
        UPDATE ces_service.vehicles
        SET    current_km = NEW.value,
               last_km_at = NEW.created_at
        WHERE  id = NEW.vehicle_id;
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_vehicle_meter
    AFTER INSERT ON ces_service.meter_readings
    FOR EACH ROW EXECUTE FUNCTION ces_service.sync_vehicle_meter();

-- ─────────────────────────────────────────────────────────────────────────────
-- Texnika üzrə fərdi baxım planı (garage_maintenance_template_items-in tətbiq olunmuş nüsxəsi,
-- ya da texnikaya birbaşa əlavə edilmiş sətir). Şablon dəyişikliyi mövcud plana avtomatik
-- tətbiq olunmur (brif) — buna görə sütunlar kopyalanır, FK ilə şablona bağlanmır.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.vehicle_maintenance_plans (
    id                      UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id               UUID          NOT NULL REFERENCES ces_service.branches(id),
    vehicle_id              UUID          NOT NULL REFERENCES ces_service.vehicles(id),
    maintenance_type        VARCHAR(100)  NOT NULL,
    interval_meter_hours    NUMERIC(10, 1) NULL,
    interval_km             NUMERIC(10, 1) NULL,
    interval_calendar_days  INTEGER       NULL,
    -- Son baxımın "bazası" — plan yaradılanda texnikanın cari göstəricisi/tarixi ilə doldurulur,
    -- tamamlanma ilə irəliləyir. Növbəti baxım bundan + interval-dan hesablanır (servis qatında).
    last_done_engine_hours  NUMERIC(10, 1) NULL,
    last_done_km            NUMERIC(10, 1) NULL,
    last_done_date          DATE          NULL,
    next_due_engine_hours   NUMERIC(10, 1) NULL,
    next_due_km             NUMERIC(10, 1) NULL,
    next_due_date           DATE          NULL,
    source_template_item_id UUID          NULL REFERENCES ces_service.garage_maintenance_template_items(id),
    is_active               BOOLEAN       NOT NULL DEFAULT TRUE,
    notes                   TEXT          NULL,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by              UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by              UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at              TIMESTAMPTZ   NULL,
    CONSTRAINT chk_vmp_has_interval CHECK (
        interval_meter_hours IS NOT NULL OR interval_km IS NOT NULL OR interval_calendar_days IS NOT NULL
    )
);
CREATE INDEX idx_vmp_vehicle ON ces_service.vehicle_maintenance_plans (vehicle_id)
    WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- Baxımın tamamlanması (Manual yol — Servis modulu hələ yoxdur, "Servis vasitəsilə" yolu o
-- modul qurulanda əlavə olunacaq source_ref_id kimi eyni sütunu istifadə edərək).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.vehicle_maintenance_completions (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id         UUID          NOT NULL REFERENCES ces_service.branches(id),
    plan_id           UUID          NOT NULL REFERENCES ces_service.vehicle_maintenance_plans(id),
    vehicle_id        UUID          NOT NULL REFERENCES ces_service.vehicles(id),
    completed_at      DATE          NOT NULL DEFAULT CURRENT_DATE,
    meter_engine_hours NUMERIC(10, 1) NULL,
    meter_km          NUMERIC(10, 1) NULL,
    -- Görülən iş + istifadə olunan materiallar: Anbar/Stok inteqrasiyası (stok çıxışı) hələ
    -- qurulmayıb, bu mərhələdə sərbəst mətndir (plan sənədi, "gələcək" bölməsi).
    description       TEXT          NULL,
    materials_notes   TEXT          NULL,
    notes             TEXT          NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by        UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by        UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at        TIMESTAMPTZ   NULL
);
CREATE INDEX idx_vmc_plan ON ces_service.vehicle_maintenance_completions (plan_id, completed_at DESC);
CREATE INDEX idx_vmc_vehicle ON ces_service.vehicle_maintenance_completions (vehicle_id, completed_at DESC);
