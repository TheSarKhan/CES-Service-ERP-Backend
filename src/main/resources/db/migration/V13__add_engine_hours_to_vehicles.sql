-- V13__add_engine_hours_to_vehicles.sql
-- Purpose: M08 — add fast-read engine-hour columns to vehicles + sync trigger (SRS M08.1).
-- After each engine_hour_logs INSERT, vehicles.current_engine_hours is updated.

ALTER TABLE ces_service.vehicles
    ADD COLUMN current_engine_hours NUMERIC(10,1) NULL DEFAULT 0,
    ADD COLUMN last_engine_hours_at TIMESTAMPTZ   NULL;

CREATE OR REPLACE FUNCTION ces_service.sync_vehicle_engine_hours()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE ces_service.vehicles
    SET    current_engine_hours = NEW.hours_value,
           last_engine_hours_at = NEW.created_at
    WHERE  id = NEW.vehicle_id;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sync_engine_hours
    AFTER INSERT ON ces_service.engine_hour_logs
    FOR EACH ROW EXECUTE FUNCTION ces_service.sync_vehicle_engine_hours();
