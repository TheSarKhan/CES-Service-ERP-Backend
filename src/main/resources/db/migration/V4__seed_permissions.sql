-- V4__seed_permissions.sql
-- Purpose: Seed the full permission catalog (SRS M16.2, pages 74-80) into ces_service.permissions.
-- created_by/updated_by use the fixed system actor UUID '00000000-0000-0000-0000-000000000000'.
-- (created_by is nullable + not FK-constrained, so the synthetic system actor is allowed.)
-- This is the authoritative 51-permission catalog. perm_type IN (CRUD, BUSINESS, REPORT, SYSTEM).

INSERT INTO ces_service.permissions (code, name, description, module, perm_type, http_method, created_by, updated_by) VALUES
-- ── VEHICLE (Qaraj) ──────────────────────────────────────────────────────────
('VEHICLE_READ',         'Texnika Oxu',            'Texnika siyahısı + detalı',                       'VEHICLE',        'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('VEHICLE_CREATE',       'Texnika Yarat',          'Yeni texnika',                                    'VEHICLE',        'CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('VEHICLE_UPDATE',       'Texnika Yenilə',         'Texnika yeniləmə + status dəyişikliyi',           'VEHICLE',        'CRUD',     'PUT',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('VEHICLE_DELETE',       'Texnika Sil',            'Texnika soft delete',                             'VEHICLE',        'CRUD',     'DELETE', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('VEHICLE_OVERRIDE',     'Texnika Override',       'IN_SERVICE texnikaya override ilə yeni sorğu',    'VEHICLE',        'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── CUSTOMER (Müştəri) ───────────────────────────────────────────────────────
('CUSTOMER_READ',        'Müştəri Oxu',            'Müştəri siyahısı + detalı',                       'CUSTOMER',       'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('CUSTOMER_CREATE',      'Müştəri Yarat',          'Yeni müştəri + ERP sinxron',                      'CUSTOMER',       'CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('CUSTOMER_UPDATE',      'Müştəri Yenilə',         'Müştəri yeniləmə + ERP sinxron',                  'CUSTOMER',       'CRUD',     'PUT',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('CUSTOMER_DELETE',      'Müştəri Sil',            'Müştəri soft delete',                             'CUSTOMER',       'CRUD',     'DELETE', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('CUSTOMER_SYNC',        'Müştəri Sinxron',        'ERP-dən manual sinxronizasiya başlat',            'CUSTOMER',       'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── SERVICE REQUEST (Servis Sorğusu) ─────────────────────────────────────────
('SR_READ',              'Sorğu Oxu',              'Sorğu siyahısı + detalı',                         'SERVICE_REQUEST','CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('SR_CREATE',            'Sorğu Yarat',            'Yeni sorğu yaratma',                              'SERVICE_REQUEST','CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('SR_UPDATE',            'Sorğu Yenilə',           'Sorğu yeniləmə + tapşırıq əlavəsi',               'SERVICE_REQUEST','CRUD',     'PUT',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('SR_DELETE',            'Sorğu Sil',              'Sorğu ləğv etmə (yalnız OPEN)',                   'SERVICE_REQUEST','CRUD',     'DELETE', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('CONTRACTOR_READ',      'Podratçı Oxu',           'Podratçılar siyahısı',                            'SERVICE_REQUEST','CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('CONTRACTOR_CREATE',    'Podratçı Yarat',         'Yeni podratçı',                                   'SERVICE_REQUEST','CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── WORK ORDER ───────────────────────────────────────────────────────────────
('WO_READ',              'WO Oxu',                 'WO siyahısı + detalı + tarixçə',                  'WORK_ORDER',     'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WO_CREATE',            'WO Yarat',               'Manuel WO yaratma',                               'WORK_ORDER',     'CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WO_UPDATE',            'WO Yenilə',              'Status dəyişikliyi, şərh, fayl',                  'WORK_ORDER',     'CRUD',     'PUT',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WO_DELETE',            'WO Sil',                 'WO soft delete',                                  'WORK_ORDER',     'CRUD',     'DELETE', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WO_CLOSE',             'WO Bağla',               'WO-nu CLOSED statusuna keçirmək',                 'WORK_ORDER',     'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WO_CANCEL',            'WO Ləğv et',             'WO-nu CANCELLED statusuna keçirmək',              'WORK_ORDER',     'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('APPROVE_COST',         'Xərc Təsdiqlə',          'WO xərclərini təsdiqləmək',                       'WORK_ORDER',     'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── COST & MARGIN ────────────────────────────────────────────────────────────
('COST_READ',            'Xərc Oxu',               'Xərc maddələri + xülasə',                         'COST',           'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('COST_MANAGE',          'Xərc İdarə',             'Xərc maddəsi əlavə/dəyişiklik/silmə',             'COST',           'CRUD',     NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('CHANGE_MARGIN',        'Marja Dəyiş',            'Marja dəyişikliyi (audit log yazılır)',           'COST',           'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('VIEW_COST_DETAILS',    'Maya Dəyəri Gör',        'Daxili maya dəyəri görmək',                       'COST',           'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('MARGIN_CONFIG_READ',   'Marja Konfiq Oxu',       'Sistem default marjalarını görmək',               'COST',           'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('MARGIN_CONFIG_UPDATE', 'Marja Konfiq Yenilə',    'Sistem default marjalarını dəyişmək',             'COST',           'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── ENGINE HOURS (Motosaat) ──────────────────────────────────────────────────
('EH_READ',              'Motosaat Oxu',           'Motosaat tarixçəsi + son dəyər',                  'ENGINE_HOURS',   'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('EH_CREATE',            'Motosaat Yarat',         'Yeni motosaat qeydiyyatı',                        'ENGINE_HOURS',   'CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('EH_ALERT_MANAGE',      'Motosaat Xəbərdarlıq',   'Motosaat xəbərdarlıqlarını idarə et',             'ENGINE_HOURS',   'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── INSPECTION (Texniki Baxış) ───────────────────────────────────────────────
('INSP_READ',            'Baxış Oxu',              'Baxış siyahısı + detalı + planlar',               'INSPECTION',     'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('INSP_CREATE',          'Baxış Yarat',            'Yeni baxış başlatma',                             'INSPECTION',     'CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('INSP_UPDATE',          'Baxış Yenilə',           'Nəticə daxil etmə + tamamlama',                   'INSPECTION',     'CRUD',     'PUT',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('INSP_MANAGE',          'Baxış İdarə',            'Checklist şablonu + baxış planı idarəsi',         'INSPECTION',     'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── WAREHOUSE (Anbar) ────────────────────────────────────────────────────────
('WH_READ',              'Anbar Oxu',              'Hissə + qovluq siyahısı, stok',                   'WAREHOUSE',      'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WH_MANAGE',            'Anbar İdarə',            'Hissə + qovluq yaratma/yeniləmə/silmə',           'WAREHOUSE',      'CRUD',     NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WH_STOCK_IN',          'Stok Giriş',             'Stoka giriş (anbar qəbulu)',                      'WAREHOUSE',      'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WH_USE',               'Stok Çıxış',             'WO üçün stok çıxışı',                             'WAREHOUSE',      'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('WH_ADJUST',            'Stok Düzəlt',            'Stok düzəltmə (sayım fərqi)',                     'WAREHOUSE',      'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── ARCHIVE, DOCUMENT, REPORT ────────────────────────────────────────────────
('ARCHIVE_READ',         'Arxiv Oxu',              'Arxiv siyahısı + detalı',                         'ARCHIVE',        'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('REOPEN_ARCHIVE',       'Arxiv Yenidən Aç',       'Arxivdən yenidən aç',                             'ARCHIVE',        'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('DOC_READ',             'Sənəd Oxu',              'Sənəd siyahısı + yükləmə',                        'DOCUMENT',       'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('DOC_CREATE',           'Sənəd Yarat',            'Sənəd generasiya et',                             'DOCUMENT',       'CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('DOC_MANAGE',           'Sənəd İdarə',            'Sənəd şablonu idarəsi',                           'DOCUMENT',       'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('REPORT_READ',          'Hesabat Oxu',            'Hesabatları görüntülə',                           'REPORT',         'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('EXPORT_REPORT',        'Hesabat İxrac',          'Hesabatları PDF/Excel-ə ixrac et',                'REPORT',         'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
-- ── ERP, BRANCH, USER, ROLE, AUDIT ───────────────────────────────────────────
('SYNC_ERP',             'ERP Sinxron',            'ERP sinxronizasiyasını idarə et',                 'ERP',            'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('BRANCH_VIEW_OWN',      'Öz Şöbəni Gör',          'Yalnız öz şöbəsini görür (default)',              'BRANCH',         'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('BRANCH_VIEW_ALL',      'Bütün Şöbələri Gör',     'Bütün şöbələri görür (CEO, Baş Mühasib)',         'BRANCH',         'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('BRANCH_MANAGE',        'Şöbə İdarə',             'Şöbə yaratma / yeniləmə',                         'BRANCH',         'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('USER_READ',            'İstifadəçi Oxu',         'İstifadəçi siyahısı + detalı',                    'USER',           'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('USER_CREATE',          'İstifadəçi Yarat',       'Yeni istifadəçi yaratma',                         'USER',           'CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('USER_UPDATE',          'İstifadəçi Yenilə',      'İstifadəçi yeniləmə + şifrə sıfırlama',           'USER',           'CRUD',     'PUT',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('USER_DELETE',          'İstifadəçi Sil',         'İstifadəçi soft delete',                          'USER',           'CRUD',     'DELETE', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('USER_ROLE_MANAGE',     'İstifadəçi Rol İdarə',   'İstifadəçiyə rol təyin/ləğv et',                  'USER',           'BUSINESS', NULL,     '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('ROLE_READ',            'Rol Oxu',                'Rol siyahısı + icazə matrisi',                    'ROLE',           'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('ROLE_CREATE',          'Rol Yarat',              'Yeni rol yaratma',                                'ROLE',           'CRUD',     'POST',   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('ROLE_UPDATE',          'Rol Yenilə',             'Rol yeniləmə + icazə dəyişikliyi',                'ROLE',           'CRUD',     'PUT',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('ROLE_DELETE',          'Rol Sil',                'Rol silmə (is_system = false)',                   'ROLE',           'CRUD',     'DELETE', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('AUDIT_READ',           'Audit Oxu',              'Audit log görüntüləmə + export',                  'AUDIT',          'CRUD',     'GET',    '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000');
