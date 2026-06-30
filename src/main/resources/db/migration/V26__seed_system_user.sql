-- ============================================================================
-- V26 — System actor user
--
-- JPA auditing stamps created_by/updated_by with the system UUID
-- (00000000-0000-0000-0000-000000000000) whenever there is no authenticated
-- user in the SecurityContext (e.g. the last-login update during /auth/login,
-- and scheduled/system jobs). users.created_by / users.updated_by carry FKs to
-- users(id), so that UUID must resolve to a real row.
--
-- This inserts a non-login, soft-deleted "System" user with that fixed id so the
-- audit FKs are always satisfiable. It is:
--   * is_active = false      -> cannot authenticate
--   * deleted_at set         -> hidden from all app queries (@Where deleted_at IS NULL)
--   * password_hash = '!'    -> never matches a bcrypt check
-- ============================================================================
INSERT INTO ces_service.users (
    id,
    branch_id,
    full_name,
    email,
    password_hash,
    is_active,
    failed_attempts,
    deleted_at,
    created_at,
    updated_at,
    created_by,
    updated_by
) VALUES (
    '00000000-0000-0000-0000-000000000000',
    '11111111-1111-1111-1111-111111111111',   -- default HQ branch (seeded in V2/V5)
    'System',
    'system@ces.local',
    '!',                                        -- unusable hash; system user never logs in
    FALSE,
    0,
    '2000-01-01 00:00:00+00',                   -- soft-deleted => excluded from app queries
    NOW(),
    NOW(),
    NULL,                                       -- created_by/updated_by nullable for the seed row
    NULL
)
ON CONFLICT (id) DO NOTHING;
