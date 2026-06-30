-- V25__seed_margin_config.sql
-- Purpose: Seed the 3 default margin_config rows for the default seed branch (HQ) (SRS §93 V25):
--   LABOR            default 20, min 0
--   PART             default 25, min 0
--   EXTERNAL_SERVICE default 15, min 0
-- margin_config.updated_by is NOT NULL FK to users, so the bootstrap admin user is used.

INSERT INTO ces_service.margin_config (branch_id, item_type, default_margin, min_margin, updated_by)
VALUES
('11111111-1111-1111-1111-111111111111', 'LABOR',            20, 0, '22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111', 'PART',             25, 0, '22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111', 'EXTERNAL_SERVICE', 15, 0, '22222222-2222-2222-2222-222222222222');
