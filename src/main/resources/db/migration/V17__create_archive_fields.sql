-- V17__create_archive_fields.sql
-- Purpose: M09 — add archive columns to work_orders + index + auto-archive trigger (SRS M09.1).
-- When a work order moves to CLOSED, it is automatically marked archived.

ALTER TABLE ces_service.work_orders
    ADD COLUMN is_archived      BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN archived_at      TIMESTAMPTZ NULL,
    ADD COLUMN archived_by      UUID        NULL REFERENCES ces_service.users(id),
    ADD COLUMN reopen_count     INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN last_reopened_at TIMESTAMPTZ NULL,
    ADD COLUMN last_reopened_by UUID        NULL REFERENCES ces_service.users(id);

CREATE INDEX idx_wo_archived
    ON ces_service.work_orders(branch_id, is_archived, closed_at DESC)
    WHERE deleted_at IS NULL;

CREATE OR REPLACE FUNCTION ces_service.auto_archive_on_close()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'CLOSED' AND OLD.status != 'CLOSED' THEN
        NEW.is_archived := TRUE;
        NEW.archived_at := NOW();
        NEW.archived_by := NEW.updated_by;
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auto_archive
    BEFORE UPDATE ON ces_service.work_orders
    FOR EACH ROW EXECUTE FUNCTION ces_service.auto_archive_on_close();
