-- V6__seed_admin_user.sql
-- Purpose: Seed the first Admin user for the default seed branch (HQ), plus the
-- user_branches row (is_default=true) and the user_roles ADMIN grant.
--
-- Fixed admin user UUID: 22222222-2222-2222-2222-222222222222
-- Default seed branch:    11111111-1111-1111-1111-111111111111  (created in V2)
-- ADMIN role:             a0000000-0000-0000-0000-000000000001   (created in V5)
--
-- Credentials (TODO: CHANGE AFTER FIRST LOGIN — see .env ADMIN_INIT_* variables):
--   email:    admin@ces.az
--   password: Admin@123
-- password_hash below is a valid bcrypt $2a$10 hash of 'Admin@123'.
-- Flyway cannot read environment variables at migration time, so the bootstrap admin
-- credentials are hardcoded here; the operator MUST change the password on first login.
--
-- created_by is left NULL (this is the first/bootstrap admin — SRS M15.1 allows NULL created_by).

-- ── First admin user ─────────────────────────────────────────────────────────
INSERT INTO ces_service.users (
    id, branch_id, full_name, email, password_hash, phone, position,
    is_active, failed_attempts, created_by, updated_by
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'System Administrator',
    'admin@ces.az',
    '$2a$10$9YZDvnNqsNhWG741oTORSO0nCLIzu2lIGUx1Cs7wo/ffvIQKZeKoe',  -- bcrypt('Admin@123')
    NULL,
    'Administrator',
    TRUE,
    0,
    NULL,   -- bootstrap admin: no creator
    NULL
);

-- ── user_branches: default branch for the admin ──────────────────────────────
INSERT INTO ces_service.user_branches (user_id, branch_id, is_default)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    TRUE
);

-- ── user_roles: grant ADMIN in the default branch ────────────────────────────
INSERT INTO ces_service.user_roles (user_id, role_id, branch_id, created_by)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'a0000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    '00000000-0000-0000-0000-000000000000'
);

-- ── Backfill: attribute the default seed branch (V2) to the real admin user now ──
-- branches.created_by has an FK to users(id), so we attribute to the admin user that
-- now exists (NOT the synthetic system actor, which is not a real users row).
UPDATE ces_service.branches
SET created_by = '22222222-2222-2222-2222-222222222222',
    updated_by = '22222222-2222-2222-2222-222222222222'
WHERE id = '11111111-1111-1111-1111-111111111111'
  AND created_by IS NULL;
