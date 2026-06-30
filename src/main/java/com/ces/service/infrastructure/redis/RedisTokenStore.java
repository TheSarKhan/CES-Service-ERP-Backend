package com.ces.service.infrastructure.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * Redis-backed store for JWT lifecycle state (SRS §4.1, §95):
 * <ul>
 *     <li>blacklist of revoked access tokens, keyed by {@code jti};</li>
 *     <li>refresh tokens, keyed by {@code userId:jti}.</li>
 * </ul>
 */
@Component
public class RedisTokenStore {

    private static final String REVOKED = "1";

    private final StringRedisTemplate redis;

    public RedisTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    // ── Blacklist (revoked access tokens) ────────────────────────────────────

    /**
     * Add an access-token {@code jti} to the blacklist with a TTL equal to the
     * remaining lifetime of the token.
     */
    public void blacklist(String jti, Duration ttl) {
        if (jti == null) {
            return;
        }
        Duration effective = ttl == null || ttl.isNegative() || ttl.isZero() ? Duration.ofMinutes(1) : ttl;
        redis.opsForValue().set(CacheKeys.blacklist(jti), REVOKED, effective);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(CacheKeys.blacklist(jti)));
    }

    // ── Refresh tokens ────────────────────────────────────────────────────────

    public void storeRefresh(UUID userId, String jti, Duration ttl) {
        redis.opsForValue().set(CacheKeys.refresh(userId, jti), REVOKED, ttl);
    }

    public boolean isRefreshValid(UUID userId, String jti) {
        return Boolean.TRUE.equals(redis.hasKey(CacheKeys.refresh(userId, jti)));
    }

    public void revokeRefresh(UUID userId, String jti) {
        redis.delete(CacheKeys.refresh(userId, jti));
    }

    /**
     * Revoke every refresh token belonging to a user (e.g. on logout-all or a
     * password change).
     */
    public void revokeAllRefresh(UUID userId) {
        Set<String> keys = redis.keys(CacheKeys.refreshPattern(userId));
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
