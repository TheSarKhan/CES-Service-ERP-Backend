-- V54__vehicle_document_number_sequence.sql
-- Auto-generates vehicle_documents.doc_number (SND-000001, ...) instead of asking the uploader to
-- type one — same pattern as vehicles.code / trg_vehicle_code in V46. The column stays writable at
-- the DB level (trigger only fills it in when NULL) so a future caller could still set a real
-- external reference number explicitly; the app itself no longer offers that input.

CREATE SEQUENCE ces_service.vehicle_document_number_seq START 1;

CREATE OR REPLACE FUNCTION ces_service.generate_vehicle_document_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.doc_number IS NULL THEN
        NEW.doc_number := 'SND-' || LPAD(nextval('ces_service.vehicle_document_number_seq')::TEXT, 6, '0');
    END IF;
    RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER trg_vehicle_document_number
    BEFORE INSERT ON ces_service.vehicle_documents
    FOR EACH ROW EXECUTE FUNCTION ces_service.generate_vehicle_document_number();
