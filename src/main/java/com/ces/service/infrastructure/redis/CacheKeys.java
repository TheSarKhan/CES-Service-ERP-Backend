package com.ces.service.infrastructure.redis;

import java.util.UUID;

/**
 * Centralised Redis key builders (SRS §95.1). All keys follow the
 * {@code namespace:context:identifier} convention. TTLs are applied by the
 * caller / {@code RedisConfig} cache manager — these helpers only build keys.
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    // ── Auth ────────────────────────────────────────────────────────────────
    /** auth:blacklist:{jti} — revoked token JTI (TTL = token exp). */
    public static String blacklist(String jti) {
        return "auth:blacklist:" + jti;
    }

    /** auth:refresh:{userId}:{jti} — refresh token store (TTL = 7 days). */
    public static String refresh(UUID userId, String jti) {
        return "auth:refresh:" + userId + ":" + jti;
    }

    /** auth:refresh:{userId}:* — pattern matching every refresh token of a user. */
    public static String refreshPattern(UUID userId) {
        return "auth:refresh:" + userId + ":*";
    }

    /** auth:ratelimit:{ip} — brute-force attempt counter (TTL = 15 min). */
    public static String rateLimit(String ip) {
        return "auth:ratelimit:" + ip;
    }

    /** auth:locked:{email} — locked account flag (TTL = 15 min). */
    public static String locked(String email) {
        return "auth:locked:" + email;
    }

    // ── Permissions / session ────────────────────────────────────────────────
    /** perm:{userId}:{branchId} — user permission list (TTL = 15 min). */
    public static String permissions(UUID userId, UUID branchId) {
        return "perm:" + userId + ":" + branchId;
    }

    /** session:{userId} — active session metadata (TTL = 1 hour). */
    public static String session(UUID userId) {
        return "session:" + userId;
    }

    // ── Caches ────────────────────────────────────────────────────────────────
    /** cache:dashboard:{branchId} — dashboard KPI summary (TTL = 60 s). */
    public static String dashboard(UUID branchId) {
        return "cache:dashboard:" + branchId;
    }

    /** cache:customers:{branchId}:{page} — ERP customer list cache (TTL = 5 min). */
    public static String customers(UUID branchId, int page) {
        return "cache:customers:" + branchId + ":" + page;
    }

    /** cache:eh:latest:{vehicleId} — latest engine-hours value (TTL = 10 min). */
    public static String engineHoursLatest(UUID vehicleId) {
        return "cache:eh:latest:" + vehicleId;
    }

    /** cache:lowstock:{branchId} — low-stock list (TTL = 5 min). */
    public static String lowStock(UUID branchId) {
        return "cache:lowstock:" + branchId;
    }

    // ── ERP / notifications ───────────────────────────────────────────────────
    /** erp:retry:{syncLogId} — ERP retry queue item (TTL = 24 hours). */
    public static String erpRetry(UUID syncLogId) {
        return "erp:retry:" + syncLogId;
    }

    /** notif:unread:{userId} — unread notification count (TTL = 5 min). */
    public static String notifUnread(UUID userId) {
        return "notif:unread:" + userId;
    }
}
