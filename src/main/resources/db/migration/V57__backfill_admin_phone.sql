-- V57__backfill_admin_phone.sql
-- Purpose: Set default phone number for bootstrap System Administrator if NULL

UPDATE ces_service.users
SET phone = '+994 12 000 00 00'
WHERE id = '22222222-2222-2222-2222-222222222222'
  AND phone IS NULL;
