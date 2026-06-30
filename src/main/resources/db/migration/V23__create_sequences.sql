-- V23__create_sequences.sql
-- Purpose: Auto-numbering for service_requests, work_orders, inspections (SRS M05.1 pattern).
-- Creates sequences sr_number_seq, wo_number_seq, ins_number_seq and the BEFORE INSERT
-- trigger functions that populate request_number / wo_number / inspection_number with the
-- formats SR-YYYY-0001, WO-YYYY-0001, INS-YYYY-0001.

-- ─────────────────────────────────────────────────────────────────────────────
-- Sequences
-- ─────────────────────────────────────────────────────────────────────────────
CREATE SEQUENCE ces_service.sr_number_seq  START 1;
CREATE SEQUENCE ces_service.wo_number_seq  START 1;
CREATE SEQUENCE ces_service.ins_number_seq START 1;

-- ─────────────────────────────────────────────────────────────────────────────
-- service_requests -> SR-YYYY-0001
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION ces_service.generate_sr_number()
RETURNS TRIGGER AS $$
BEGIN
    NEW.request_number := 'SR-' || TO_CHAR(NOW(),'YYYY') || '-'
        || LPAD(nextval('ces_service.sr_number_seq')::TEXT, 4, '0');
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sr_number
    BEFORE INSERT ON ces_service.service_requests
    FOR EACH ROW EXECUTE FUNCTION ces_service.generate_sr_number();

-- ─────────────────────────────────────────────────────────────────────────────
-- work_orders -> WO-YYYY-0001
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION ces_service.generate_wo_number()
RETURNS TRIGGER AS $$
BEGIN
    NEW.wo_number := 'WO-' || TO_CHAR(NOW(),'YYYY') || '-'
        || LPAD(nextval('ces_service.wo_number_seq')::TEXT, 4, '0');
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_wo_number
    BEFORE INSERT ON ces_service.work_orders
    FOR EACH ROW EXECUTE FUNCTION ces_service.generate_wo_number();

-- ─────────────────────────────────────────────────────────────────────────────
-- inspections -> INS-YYYY-0001
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION ces_service.generate_ins_number()
RETURNS TRIGGER AS $$
BEGIN
    NEW.inspection_number := 'INS-' || TO_CHAR(NOW(),'YYYY') || '-'
        || LPAD(nextval('ces_service.ins_number_seq')::TEXT, 4, '0');
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ins_number
    BEFORE INSERT ON ces_service.inspections
    FOR EACH ROW EXECUTE FUNCTION ces_service.generate_ins_number();
