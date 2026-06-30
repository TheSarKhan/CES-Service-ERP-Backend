-- V3__create_rbac_tables.sql
-- Purpose: RBAC tables: roles, permissions, role_permissions, user_roles (SRS M16.1).
--
-- Seed support: created_by / updated_by on roles and permissions are NULLABLE so that the
-- permission catalog (V4) and default roles (V5) can be seeded with the system actor UUID
-- '00000000-0000-0000-0000-000000000000' (a synthetic actor; not a real users row).
-- permissions has NO branch_id (global catalog). user_roles PK is (user_id, role_id, branch_id).

-- ─────────────────────────────────────────────────────────────────────────────
-- roles
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.roles (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    name         VARCHAR(100)  NOT NULL,    -- "Servis Meneceri", "Mexanik"
    code         VARCHAR(100)  NOT NULL,    -- "SERVICE_MANAGER", "MECHANIC"
    description  TEXT          NULL,
    is_system    BOOLEAN       NOT NULL DEFAULT FALSE,   -- system role cannot be deleted
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ   NULL,
    created_by   UUID          NULL,   -- nullable for seed; not FK-constrained (system actor)
    updated_by   UUID          NULL,
    UNIQUE (branch_id, code)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- permissions  (global catalog — no branch_id)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.permissions (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(100)  NOT NULL UNIQUE,   -- "WO_CREATE", "CHANGE_MARGIN"
    name         VARCHAR(255)  NOT NULL,
    description  TEXT          NULL,
    module       VARCHAR(100)  NOT NULL,          -- "WORK_ORDER", "COST"
    perm_type    VARCHAR(50)   NOT NULL
                 CHECK (perm_type IN ('CRUD','BUSINESS','REPORT','SYSTEM')),
    http_method  VARCHAR(20)   NULL,              -- GET|POST|PUT|DELETE (CRUD only)
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by   UUID          NULL,   -- nullable for seed; not FK-constrained (system actor)
    updated_by   UUID          NULL
);

-- ─────────────────────────────────────────────────────────────────────────────
-- role_permissions  (role <-> permission)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.role_permissions (
    role_id        UUID         NOT NULL REFERENCES ces_service.roles(id)       ON DELETE CASCADE,
    permission_id  UUID         NOT NULL REFERENCES ces_service.permissions(id) ON DELETE CASCADE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by     UUID         NULL,   -- nullable for seed (system actor)
    PRIMARY KEY (role_id, permission_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- user_roles  (user <-> role <-> branch)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.user_roles (
    user_id      UUID         NOT NULL REFERENCES ces_service.users(id)    ON DELETE CASCADE,
    role_id      UUID         NOT NULL REFERENCES ces_service.roles(id)    ON DELETE CASCADE,
    branch_id    UUID         NOT NULL REFERENCES ces_service.branches(id),  -- role for this branch
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   UUID         NULL,   -- nullable so first admin assignment (V6) can seed
    PRIMARY KEY (user_id, role_id, branch_id)
);
