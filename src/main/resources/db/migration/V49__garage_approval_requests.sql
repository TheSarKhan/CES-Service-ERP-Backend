-- V49__garage_approval_requests.sql
-- Splits Qaraj/Motosaat's approval queue out of Anbar's approval_requests table (V34) into its
-- own table — the user's explicit call: the two must not mix, and Qaraj gets its own sidebar
-- module/page rather than sharing Anbarın Təsdiqləmələr screen. Anbar's own stock-out approvals
-- (including the one Motosaat's maintenance-completion materials submit) are unaffected — a
-- material consumption is a real Anbar stock movement and correctly stays in Anbar's own queue.
--
-- Same shape as approval_requests (V34) — see that migration's header for the mechanism itself,
-- unchanged here: submit → PENDING → a second, distinct person decides.

CREATE TABLE ces_service.garage_approval_requests (
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

    CONSTRAINT garage_approval_requests_status_chk
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE UNIQUE INDEX ux_garage_approval_requests_pending_entity
    ON ces_service.garage_approval_requests (entity_type, entity_id)
    WHERE status = 'PENDING' AND deleted_at IS NULL;

CREATE INDEX ix_garage_approval_requests_branch_status
    ON ces_service.garage_approval_requests (branch_id, status, requested_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX ix_garage_approval_requests_requested_by
    ON ces_service.garage_approval_requests (requested_by)
    WHERE deleted_at IS NULL;

-- Move any existing VEHICLE / METER_READING rows out of the shared table rather than losing
-- history — harmless no-op if none exist yet (fresh modules, no real approvals decided so far).
INSERT INTO ces_service.garage_approval_requests (
    id, branch_id, entity_type, entity_id, entity_label, operation, status,
    payload, before_snapshot, requested_by, requested_by_name, requested_at,
    decided_by, decided_by_name, decided_at, decision_note,
    created_at, updated_at, deleted_at, created_by, updated_by)
SELECT
    id, branch_id, entity_type, entity_id, entity_label, operation, status,
    payload, before_snapshot, requested_by, requested_by_name, requested_at,
    decided_by, decided_by_name, decided_at, decision_note,
    created_at, updated_at, deleted_at, created_by, updated_by
FROM ces_service.approval_requests
WHERE entity_type IN ('VEHICLE', 'METER_READING');

DELETE FROM ces_service.approval_requests WHERE entity_type IN ('VEHICLE', 'METER_READING');

-- ── İcazələr ────────────────────────────────────────────────────────────────
INSERT INTO ces_service.permissions (code, name, description, module, perm_type, http_method, created_by, updated_by) VALUES
('GARAGE_APPROVAL_READ',   'Qaraj Təsdiq Oxu',    'Qaraj təsdiq sorğuları siyahısı + detalı',         'GARAGE', 'CRUD',     'GET', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000'),
('GARAGE_APPROVAL_DECIDE', 'Qaraj Təsdiq Qərarı', 'Qaraj təsdiq sorğusunu təsdiqləmək / imtina etmək','GARAGE', 'BUSINESS', NULL,  '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000');

-- Same role split as APPROVAL_READ/APPROVAL_DECIDE (V34): Admin/Servis Meneceri/Direktor decide,
-- everyone who could already see Anbarın Təsdiqləmələr can at least read Qarajınkını too.
INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT r.role_id, p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
CROSS JOIN (VALUES
    ('a0000000-0000-0000-0000-000000000001'::uuid),
    ('a0000000-0000-0000-0000-000000000002'::uuid),
    ('a0000000-0000-0000-0000-000000000006'::uuid)
) AS r(role_id)
WHERE p.code IN ('GARAGE_APPROVAL_READ', 'GARAGE_APPROVAL_DECIDE');

INSERT INTO ces_service.role_permissions (role_id, permission_id, created_by)
SELECT 'a0000000-0000-0000-0000-000000000005', p.id, '00000000-0000-0000-0000-000000000000'
FROM ces_service.permissions p
WHERE p.code = 'GARAGE_APPROVAL_READ';
