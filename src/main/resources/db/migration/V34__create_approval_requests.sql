-- V34__create_approval_requests.sql
-- Purpose: paralel təsdiqləmə mexanizmi. Anbarda dağıdıcı əməliyyat (redaktə, silmə, stok
-- hərəkəti, qovluq/kateqoriya dəyişikliyi) dərhal icra olunmur — PENDING sorğu yaradılır,
-- ikinci səlahiyyətli şəxs təsdiqləyəndə icra edilir.
--
-- Əsas qayda: sorğunu açan şəxs onu ÖZÜ təsdiqləyə bilməz (servis qatında yoxlanılır).
--
-- `payload` icra üçün lazım olan sorğu gövdəsidir (təsdiq anında yenidən oynadılır),
-- `before_snapshot` isə fərqi göstərmək üçün əməliyyatdan əvvəlki vəziyyətdir. Obyekt
-- silindikdən sonra da siyahıda oxunaqlı qalsın deyə `entity_label` ayrıca saxlanılır.

CREATE TABLE ces_service.approval_requests (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id         UUID         NOT NULL,

    entity_type       VARCHAR(50)  NOT NULL,
    entity_id         UUID         NOT NULL,
    entity_label      VARCHAR(255),
    operation         VARCHAR(50)  NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',

    payload           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    before_snapshot   JSONB,

    requested_by      UUID,
    requested_by_name VARCHAR(255),
    requested_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    decided_by        UUID,
    decided_by_name   VARCHAR(255),
    decided_at        TIMESTAMPTZ,
    decision_note     TEXT,

    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    created_by        UUID,
    updated_by        UUID,

    CONSTRAINT approval_requests_status_chk
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

-- Kilid: bir obyektin eyni anda yalnız BİR gözləyən sorğusu ola bilər. Bu, «təsdiq gözləyərkən
-- obyekt kilidlənsin» qaydasını baza səviyyəsində təmin edir — servis yoxlaması ilə yanaşı,
-- paralel iki sorğunun yarışını da bloklayır.
CREATE UNIQUE INDEX ux_approval_requests_pending_entity
    ON ces_service.approval_requests (entity_type, entity_id)
    WHERE status = 'PENDING' AND deleted_at IS NULL;

CREATE INDEX ix_approval_requests_branch_status
    ON ces_service.approval_requests (branch_id, status, requested_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_approval_requests_requested_by
    ON ces_service.approval_requests (requested_by)
    WHERE deleted_at IS NULL;

-- ── İcazələr ────────────────────────────────────────────────────────────────
INSERT INTO ces_service.permissions (code, name, description, module, perm_type, http_method, created_by, updated_by) VALUES
('APPROVAL_READ',   'Təsdiq Oxu',      'Təsdiq sorğuları siyahısı + detalı',        'APPROVAL', 'CRUD',     'GET',  '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('APPROVAL_DECIDE', 'Təsdiq Qərarı',   'Təsdiq sorğusunu təsdiqləmək / imtina etmək','APPROVAL', 'BUSINESS', NULL,   '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000');

-- ADMIN bütün icazələri alır (V5-dəki qayda yeni icazələrə avtomatik şamil olunmur,
-- ona görə burada açıq şəkildə əlavə edilir).
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000001', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code IN ('APPROVAL_READ', 'APPROVAL_DECIDE');

-- Servis Meneceri və Direktor təsdiq qərarı verə bilir; Anbardar yalnız öz sorğularını görür.
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000002', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code IN ('APPROVAL_READ', 'APPROVAL_DECIDE');

INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000006', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code IN ('APPROVAL_READ', 'APPROVAL_DECIDE');

INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000005', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code = 'APPROVAL_READ';
