-- V56__garage_settings.sql
-- Branch-scoped numeric Motosaat settings that don't fit garage_config_values (that table is for
-- dropdown-list VALUES, these are scalar thresholds) — per the Motosaat brief's "Anormal motosaat
-- artımı" / "Motosaat yenilənməmə limiti" sections, both explicitly "Konfiqurasiya modulundan
-- təyin edilə bilər". One row per branch; all columns nullable — a branch that never visits
-- Konfiqurasiya simply has the feature switched off rather than some hardcoded default surprising
-- them later.

CREATE TABLE ces_service.garage_settings (
    id                                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id                          UUID          NOT NULL REFERENCES ces_service.branches(id),
    -- Neçə gün ərzində yeni oxunuş qeyd olunmayıbsa, texnika "yenilənməmiş" sayılır.
    stale_reading_days                 INTEGER       NULL,
    -- Bir qeyddə əvvəlki dəyərdən bu qədər artıq fərq varsa, "anormal artım" xəbərdarlığı göstərilir
    -- (bloklamır — istifadəçi təsdiqləyib davam edə bilər, brifin öz sözü ilə).
    max_normal_increase_engine_hours   NUMERIC(10, 1) NULL,
    max_normal_increase_km             NUMERIC(10, 1) NULL,
    created_at                         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by                         UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at                         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by                         UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at                         TIMESTAMPTZ   NULL
);
CREATE UNIQUE INDEX ux_garage_settings_branch
    ON ces_service.garage_settings(branch_id)
    WHERE deleted_at IS NULL;
