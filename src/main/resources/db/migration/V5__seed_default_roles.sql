-- V5__seed_default_roles.sql
-- Purpose: Seed the 6 system roles (is_system=true) for the default seed branch (HQ),
-- then seed role_permissions per the SRS M16.3 matrix (pages 78-80).
--   ADMIN           -> ALL permissions
--   SERVICE_MANAGER -> exactly the ServisMeneceri column ✓ cells
--   MECHANIC        -> exactly the Mexanik column ✓/≈ cells (≈ counts as a grant; row-scoping is app-layer)
--   ACCOUNTANT      -> exactly the Mühasib column ✓ cells
--   WAREHOUSE       -> exactly the Anbardar column ✓ cells
--   DIRECTOR        -> exactly the Direktor column ✓ cells
--
-- All roles belong to the default seed branch 11111111-1111-1111-1111-111111111111.
-- created_by/updated_by use the system actor UUID '00000000-0000-0000-0000-000000000000'.
-- Fixed role UUIDs are used so V6 can reference the ADMIN role deterministically.

-- ─────────────────────────────────────────────────────────────────────────────
-- 6 system roles (fixed UUIDs)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO ces_service.roles (id, branch_id, name, code, description, is_system, is_active, created_by, updated_by) VALUES
('a0000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Administrator',    'ADMIN',           'Tam sistem girişi — bütün icazələr', TRUE, TRUE, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('a0000000-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Servis Meneceri',  'SERVICE_MANAGER', 'Servis əməliyyatlarının idarəsi',     TRUE, TRUE, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('a0000000-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'Mexanik',          'MECHANIC',        'Texniki icra və motosaat',            TRUE, TRUE, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('a0000000-0000-0000-0000-000000000004', '11111111-1111-1111-1111-111111111111', 'Mühasib',          'ACCOUNTANT',      'Maliyyə, xərc təsdiqi, hesabatlar',   TRUE, TRUE, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('a0000000-0000-0000-0000-000000000005', '11111111-1111-1111-1111-111111111111', 'Anbardar',         'WAREHOUSE',       'Anbar və stok idarəetməsi',           TRUE, TRUE, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('a0000000-0000-0000-0000-000000000006', '11111111-1111-1111-1111-111111111111', 'Direktor',         'DIRECTOR',        'İcmal görünüş — bütün şöbələr',       TRUE, TRUE, '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000');

-- ─────────────────────────────────────────────────────────────────────────────
-- ADMIN -> ALL permissions
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000001', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p;

-- ─────────────────────────────────────────────────────────────────────────────
-- SERVICE_MANAGER (ServisMeneceri column)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000002', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code IN (
    'VEHICLE_READ','VEHICLE_CREATE','VEHICLE_UPDATE','VEHICLE_OVERRIDE',
    'CUSTOMER_READ','CUSTOMER_CREATE','CUSTOMER_UPDATE',
    'SR_READ','SR_CREATE','SR_UPDATE',
    'WO_READ','WO_UPDATE','WO_CLOSE','WO_CANCEL','APPROVE_COST',
    'COST_READ','COST_MANAGE','CHANGE_MARGIN','VIEW_COST_DETAILS',
    'EH_READ','EH_CREATE','EH_ALERT_MANAGE',
    'INSP_READ','INSP_CREATE','INSP_UPDATE','INSP_MANAGE',
    'WH_READ','WH_MANAGE','WH_STOCK_IN','WH_USE','WH_ADJUST',
    'ARCHIVE_READ','REOPEN_ARCHIVE',
    'DOC_CREATE','REPORT_READ','EXPORT_REPORT',
    'USER_READ','ROLE_READ'
);

-- ─────────────────────────────────────────────────────────────────────────────
-- MECHANIC (Mexanik column; ≈ treated as a grant)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000003', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code IN (
    'VEHICLE_READ',
    'CUSTOMER_READ',
    'SR_READ','SR_UPDATE',
    'WO_READ','WO_UPDATE',
    'EH_READ','EH_CREATE',
    'INSP_READ','INSP_CREATE','INSP_UPDATE',
    'WH_READ','WH_USE'
);

-- ─────────────────────────────────────────────────────────────────────────────
-- ACCOUNTANT (Mühasib column)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000004', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code IN (
    'CUSTOMER_READ',
    'SR_READ',
    'WO_READ','APPROVE_COST',
    'COST_READ','VIEW_COST_DETAILS',
    'WH_READ',
    'ARCHIVE_READ',
    'DOC_CREATE','REPORT_READ','EXPORT_REPORT'
);

-- ─────────────────────────────────────────────────────────────────────────────
-- WAREHOUSE (Anbardar column)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000005', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code IN (
    'WH_READ','WH_MANAGE','WH_STOCK_IN','WH_ADJUST'
);

-- ─────────────────────────────────────────────────────────────────────────────
-- DIRECTOR (Direktor column)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000006', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code IN (
    'VEHICLE_READ',
    'CUSTOMER_READ',
    'SR_READ',
    'WO_READ',
    'EH_READ',
    'INSP_READ',
    'WH_READ',
    'ARCHIVE_READ',
    'REPORT_READ',
    'BRANCH_VIEW_ALL'
);
