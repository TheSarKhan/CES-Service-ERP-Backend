-- V53__vehicle_parameters.sql
-- Free-form "ad: dəyər" technical parameter rows per vehicle (e.g. "Mühərrik gücü" / "150 HP").
-- Unlike Inventory's category-driven EAV attributes, Qaraj equipment types vary too widely for one
-- shared field schema (see V46's own comment on why Qaraj deliberately has no per-type dynamic
-- fields) — so this is a plain JSON array on the vehicle itself, not a new config-value list type
-- or a child table: it travels with the rest of the vehicle payload through the existing
-- create/update (approval) flow rather than needing its own endpoints.

ALTER TABLE ces_service.vehicles
    ADD COLUMN parameters JSONB NOT NULL DEFAULT '[]';
