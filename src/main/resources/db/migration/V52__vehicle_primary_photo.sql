-- V52__vehicle_primary_photo.sql
-- The Texnikalar list/table now shows a thumbnail per row — a vehicle can have many categorized
-- photos (Ön görünüş, Şassi, ...), so the mechanic picks one as the "cover" shown in lists.
-- Denormalized onto vehicles (id + resolved URL) so listing a page of vehicles never needs an
-- extra join per row, same reasoning as the existing current_engine_hours/current_km fast-read
-- caches on this table.

ALTER TABLE ces_service.vehicles
    ADD COLUMN primary_photo_id  UUID NULL REFERENCES ces_service.vehicle_photos(id) ON DELETE SET NULL,
    ADD COLUMN primary_photo_url TEXT NULL;
