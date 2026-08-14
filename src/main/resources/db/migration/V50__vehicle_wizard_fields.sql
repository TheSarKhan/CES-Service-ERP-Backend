-- V50__vehicle_wizard_fields.sql
-- Adds the finance/technical fields the "Yeni texnika əlavə et" wizard collects on its
-- "Maliyyə & Texniki" step, plus two checklist columns for its "Mülkiyyət" step. Ownership itself
-- (Şirkət/Müştəri) keeps using the existing garage_type/owner_id — no new ownership column, per
-- VehicleService.validateOwnership's own comment anticipating a real Customer picker once one
-- exists ("Once it exists, this is the place to validate ownership eagerly").

ALTER TABLE ces_service.vehicles
    ADD COLUMN purchase_date        DATE           NULL,
    ADD COLUMN purchase_price       NUMERIC(14, 2) NULL,
    ADD COLUMN market_value         NUMERIC(14, 2) NULL,
    ADD COLUMN depreciation_percent NUMERIC(5, 2)  NULL,
    ADD COLUMN safety_equipment     TEXT[]         NOT NULL DEFAULT '{}',
    ADD COLUMN mandatory_documents  TEXT[]         NOT NULL DEFAULT '{}';
