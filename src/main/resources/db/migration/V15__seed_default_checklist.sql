-- V15__seed_default_checklist.sql
-- Purpose: Seed one default inspection checklist "Standart Aylıq Baxış" (is_default=true) for
-- the default seed branch (HQ) + 15 checklist items (BRD §13.3) with JSONB options arrays.
--
-- inspection_checklists / inspection_checklist_items have NOT NULL created_by/updated_by FK to
-- users(id), so the bootstrap admin user (22222222-...) is used as the seed actor.
-- Fixed checklist UUID c0000000-0000-0000-0000-000000000001 for determinism.

INSERT INTO ces_service.inspection_checklists
    (id, branch_id, name, description, is_default, is_active, created_by, updated_by)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    'Standart Aylıq Baxış',
    'Şirkət və investor qarajındakı texnikalar üçün standart aylıq texniki baxış checklist-i',
    TRUE,
    TRUE,
    '22222222-2222-2222-2222-222222222222',
    '22222222-2222-2222-2222-222222222222'
);

INSERT INTO ces_service.inspection_checklist_items
    (branch_id, checklist_id, item_name, item_category, options, requires_value, value_unit, sort_order, created_by, updated_by)
VALUES
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Mühərrik yağı',     'Yağlama sistemi',     '["Normal","Az","Çox","Çirkli","Dəyişdirmə lazım"]'::jsonb, FALSE, NULL,    1,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Soyuducu mayesi',   'Soyutma sistemi',     '["Normal","Az","Çox","Dəyişdirmə lazım"]'::jsonb,           FALSE, NULL,    2,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Hidravlik yağ',     'Hidravlik sistem',    '["Normal","Az","Çox","Çirkli","Dəyişdirmə lazım"]'::jsonb, FALSE, NULL,    3,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Yağ süzgəci',       'Süzgəclər',           '["Normal","Çirkli","Dəyişdirmə lazım"]'::jsonb,             FALSE, NULL,    4,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Hava süzgəci',      'Süzgəclər',           '["Normal","Çirkli","Dəyişdirmə lazım"]'::jsonb,             FALSE, NULL,    5,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Yanacaq süzgəci',   'Süzgəclər',           '["Normal","Çirkli","Dəyişdirmə lazım"]'::jsonb,             FALSE, NULL,    6,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Ötürücü qutusu',    'Transmissiya',        '["Normal","Səs var","Sızıntı var","Təmir lazım"]'::jsonb,   FALSE, NULL,    7,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Əyləc sistemi',     'Əyləc',               '["Normal","Zəif","Təmir lazım","Təhlükəli"]'::jsonb,        FALSE, NULL,    8,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Şin/Təkər',         'Hərəkət sistemi',     '["Normal","Aşınmış","Dəyişdirmə lazım"]'::jsonb,            FALSE, NULL,    9,  '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Akkumulyator',      'Elektrik sistemi',    '["Normal","Zəif","Dəyişdirmə lazım"]'::jsonb,               TRUE,  'volt',  10, '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','İşıq sistemi',      'Elektrik sistemi',    '["Normal","Bir hissəsi işləmir","İşləmir"]'::jsonb,         FALSE, NULL,    11, '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Sürücü kabini',     'Kabin',               '["Normal","Qüsurlu","Təmir lazım"]'::jsonb,                 FALSE, NULL,    12, '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Kuzov/Çərçivə',     'Kuzov',               '["Normal","Cızıq var","Deformasiya var","Çat var"]'::jsonb, FALSE, NULL,    13, '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Hidravlik şlanqlar','Hidravlik sistem',    '["Normal","Sızıntı var","Dəyişdirmə lazım"]'::jsonb,        FALSE, NULL,    14, '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','c0000000-0000-0000-0000-000000000001','Cari motosaat',     'Motosaat',            '["Qeyd edildi"]'::jsonb,                                    TRUE,  'saat',  15, '22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222');
