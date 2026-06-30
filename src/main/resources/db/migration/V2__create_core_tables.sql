-- V2__create_core_tables.sql
-- Purpose: Core foundation tables: branches, users, user_branches.
--
-- Circular FK problem:
--   users.branch_id   -> branches(id)
--   users.created_by  -> users(id)      (self, nullable: first admin has NULL)
--   branches.created_by -> users(id)
-- Resolution: create branches and users WITHOUT the cross-referencing audit FKs,
-- then add the FK constraints via ALTER once both tables exist.
--
-- Seed support: created_by / updated_by on branches and users are NULLABLE so that the
-- default seed branch (V2) and the first admin (V6) can be inserted before any user exists.
-- The chosen system actor UUID is 00000000-0000-0000-0000-000000000000 and the default
-- seed branch UUID is 11111111-1111-1111-1111-111111111111 (code 'HQ', name 'Baş Ofis').

-- ─────────────────────────────────────────────────────────────────────────────
-- branches
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.branches (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255)  NOT NULL,
    code         VARCHAR(50)   NOT NULL UNIQUE,    -- e.g. "BAK", "GNJ", "HQ"
    address      TEXT          NULL,
    phone        VARCHAR(50)   NULL,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ   NULL,
    created_by   UUID          NULL,   -- nullable for seed; FK added below
    updated_by   UUID          NULL    -- nullable for seed; FK added below
);

-- ─────────────────────────────────────────────────────────────────────────────
-- users
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.users (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id        UUID          NOT NULL REFERENCES ces_service.branches(id),  -- primary branch
    full_name        VARCHAR(255)  NOT NULL,
    email            VARCHAR(255)  NOT NULL UNIQUE,
    password_hash    VARCHAR(255)  NOT NULL,    -- bcrypt
    phone            VARCHAR(50)   NULL,
    position         VARCHAR(100)  NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    last_login_at    TIMESTAMPTZ   NULL,
    failed_attempts  INTEGER       NOT NULL DEFAULT 0,
    locked_until     TIMESTAMPTZ   NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ   NULL,
    created_by       UUID          NULL,   -- NULL = first admin; self-FK added below
    updated_by       UUID          NULL
);

CREATE INDEX idx_users_email  ON ces_service.users(email)     WHERE deleted_at IS NULL;
CREATE INDEX idx_users_branch ON ces_service.users(branch_id) WHERE deleted_at IS NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- Deferred audit FK constraints (now that both tables exist)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE ces_service.branches
    ADD CONSTRAINT fk_branches_created_by FOREIGN KEY (created_by) REFERENCES ces_service.users(id),
    ADD CONSTRAINT fk_branches_updated_by FOREIGN KEY (updated_by) REFERENCES ces_service.users(id);

ALTER TABLE ces_service.users
    ADD CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES ces_service.users(id),
    ADD CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES ces_service.users(id);

-- ─────────────────────────────────────────────────────────────────────────────
-- user_branches  (user <-> branch many-to-many; active branch chosen at login)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE ces_service.user_branches (
    user_id      UUID        NOT NULL REFERENCES ces_service.users(id)    ON DELETE CASCADE,
    branch_id    UUID        NOT NULL REFERENCES ces_service.branches(id) ON DELETE CASCADE,
    is_default   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, branch_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Default seed branch (fixed UUID) reused by V5/V6/V15/V19/V25.
-- created_by/updated_by are left NULL here because the system user does not yet exist;
-- they remain NULL (first-bootstrap rows are exempt from audit attribution).
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO ces_service.branches (id, name, code, address, phone, is_active)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Baş Ofis',
    'HQ',
    'Bakı, Azərbaycan',
    NULL,
    TRUE
);
