package com.ces.service.module.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;

/**
 * Generates and validates the independent CES Service JWTs (SRS §4.1).
 *
 * <p>Access tokens carry {@code sub, email, branch_id, roles, permissions, iat,
 * exp, jti}; refresh tokens carry only {@code sub, jti, iat, exp}. Signing uses
 * HS256 with the configured secret.</p>
 */
@Service
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_BRANCH = "branch_id";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;
    private final String issuer;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs,
            @Value("${jwt.issuer:ces-service}") String issuer) {
        this.signingKey = buildKey(secret);
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.issuer = issuer;
    }

    /**
     * Build a signing key. Accepts a Base64-encoded secret; falls back to the
     * raw UTF-8 bytes when the value is not valid Base64.
     */
    private static SecretKey buildKey(String secret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ignored) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    // ── Generation ────────────────────────────────────────────────────────────

    public String generateAccessToken(UUID userId,
                                       String email,
                                       UUID branchId,
                                       List<String> roles,
                                       List<String> permissions,
                                       String jti) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(accessExpirationMs);
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .id(jti)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_BRANCH, branchId != null ? branchId.toString() : null)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMISSIONS, permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String jti) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(refreshExpirationMs);
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .id(jti)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey)
                .compact();
    }

    // ── Parsing / validation ──────────────────────────────────────────────────

    /**
     * Parse and validate a token, returning the decoded claims.
     *
     * @throws BusinessException with {@link ErrorCode#AUTH_TOKEN_EXPIRED} when
     *         expired, or {@link ErrorCode#AUTH_INVALID_CREDENTIALS} when the
     *         signature / structure is invalid.
     */
    @SuppressWarnings("unchecked")
    public JwtClaims parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            Claims c = jws.getPayload();

            UUID subject = UUID.fromString(c.getSubject());
            String branchRaw = c.get(CLAIM_BRANCH, String.class);
            UUID branchId = branchRaw != null ? UUID.fromString(branchRaw) : null;
            List<String> roles = c.get(CLAIM_ROLES, List.class);
            List<String> permissions = c.get(CLAIM_PERMISSIONS, List.class);

            return new JwtClaims(
                    subject,
                    c.get(CLAIM_EMAIL, String.class),
                    branchId,
                    roles != null ? roles : List.of(),
                    permissions != null ? permissions : List.of(),
                    c.getId(),
                    c.getIssuedAt() != null ? c.getIssuedAt().toInstant() : null,
                    c.getExpiration() != null ? c.getExpiration().toInstant() : null
            );
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, ErrorCode.AUTH_TOKEN_EXPIRED.getDefaultMessage());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid token");
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims c = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return TYPE_REFRESH.equals(c.get(CLAIM_TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }
}
