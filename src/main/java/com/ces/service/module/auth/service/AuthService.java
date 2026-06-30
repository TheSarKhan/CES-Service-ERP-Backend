package com.ces.service.module.auth.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.infrastructure.redis.RedisTokenStore;
import com.ces.service.module.auth.dto.AuthUserDto;
import com.ces.service.module.auth.dto.ForgotPasswordRequest;
import com.ces.service.module.auth.dto.LoginRequest;
import com.ces.service.module.auth.dto.RefreshRequest;
import com.ces.service.module.auth.dto.ResetPasswordRequest;
import com.ces.service.module.auth.dto.SwitchBranchRequest;
import com.ces.service.module.auth.dto.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core authentication flows (SRS §4): login, token refresh, logout (jti
 * blacklist), branch switch, and password reset entry points.
 *
 * <p>Persistence of users/roles/permissions is delegated to {@link AuthUserGateway}
 * (implemented by the RBAC module).</p>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthUserGateway userGateway;
    private final JwtService jwtService;
    private final RedisTokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthUserGateway userGateway,
                       JwtService jwtService,
                       RedisTokenStore tokenStore,
                       PasswordEncoder passwordEncoder) {
        this.userGateway = userGateway;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public TokenResponse login(LoginRequest request) {
        AuthUserGateway.AuthUser user = userGateway.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        if (user.lockedUntil() != null && user.lockedUntil().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }
        if (!user.active()) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_INACTIVE);
        }
        if (user.passwordHash() == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            userGateway.onLoginFailure(user.id());
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        userGateway.onLoginSuccess(user.id());

        UUID activeBranch = defaultBranch(user);
        return issueTokens(user, activeBranch);
    }

    // ── Refresh ─────────────────────────────────────────────────────────────────

    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Not a refresh token");
        }
        JwtClaims claims = jwtService.parse(refreshToken);

        if (!tokenStore.isRefreshValid(claims.subject(), claims.jti())) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_REVOKED);
        }

        AuthUserGateway.AuthUser user = userGateway.findById(claims.subject())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        if (!user.active()) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_INACTIVE);
        }

        // Rotate the refresh token: revoke the old one and issue a fresh pair.
        tokenStore.revokeRefresh(claims.subject(), claims.jti());
        UUID activeBranch = defaultBranch(user);
        return issueTokens(user, activeBranch);
    }

    // ── Logout ─────────────────────────────────────────────────────────────────

    /**
     * Blacklist the access token by {@code jti} and revoke the user's refresh
     * tokens (SRS §4.1).
     */
    public void logout(String accessToken) {
        JwtClaims claims = jwtService.parse(accessToken);
        Duration ttl = remainingTtl(claims);
        tokenStore.blacklist(claims.jti(), ttl);
        tokenStore.revokeAllRefresh(claims.subject());
    }

    // ── Switch branch ────────────────────────────────────────────────────────────

    public TokenResponse switchBranch(UUID userId, SwitchBranchRequest request) {
        AuthUserGateway.AuthUser user = userGateway.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        boolean member = user.branches().stream()
                .anyMatch(b -> b.branchId().equals(request.branchId()));
        if (!member) {
            throw new BusinessException(ErrorCode.BRANCH_ACCESS_DENIED);
        }
        return issueTokens(user, request.branchId());
    }

    // ── Password reset (entry points) ─────────────────────────────────────────────

    public void forgotPassword(ForgotPasswordRequest request) {
        // Always behaves as success to avoid leaking which emails exist.
        // TODO Phase 2: generate a reset token, persist it, and dispatch the email.
        log.info("Password reset requested for {}", request.email());
    }

    public void resetPassword(ResetPasswordRequest request) {
        // TODO Phase 2: validate the reset token, enforce password policy, and
        // update the user's password hash; revoke existing sessions.
        throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Password reset is not yet implemented");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private TokenResponse issueTokens(AuthUserGateway.AuthUser user, UUID activeBranch) {
        List<String> roles = userGateway.resolveRoles(user.id(), activeBranch);
        List<String> permissions = userGateway.resolvePermissions(user.id(), activeBranch);

        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();

        String accessToken = jwtService.generateAccessToken(
                user.id(), user.email(), activeBranch, roles, permissions, accessJti);
        String refreshToken = jwtService.generateRefreshToken(user.id(), refreshJti);

        tokenStore.storeRefresh(user.id(), refreshJti,
                Duration.ofMillis(jwtService.getRefreshExpirationMs()));

        AuthUserDto userDto = toUserDto(user, activeBranch, roles, permissions);
        long expiresInSeconds = jwtService.getAccessExpirationMs() / 1000;
        return TokenResponse.of(accessToken, refreshToken, expiresInSeconds, userDto);
    }

    private AuthUserDto toUserDto(AuthUserGateway.AuthUser user,
                                  UUID activeBranch,
                                  List<String> roles,
                                  List<String> permissions) {
        List<AuthUserDto.BranchSummary> branches = user.branches().stream()
                .map(b -> new AuthUserDto.BranchSummary(b.branchId(), b.branchName()))
                .toList();
        return new AuthUserDto(
                user.id(),
                user.fullName(),
                user.email(),
                activeBranch,
                branches,
                roles,
                permissions
        );
    }

    private UUID defaultBranch(AuthUserGateway.AuthUser user) {
        if (user.branches() == null || user.branches().isEmpty()) {
            throw new BusinessException(ErrorCode.BRANCH_ACCESS_DENIED, "User has no branch membership");
        }
        return user.branches().stream()
                .filter(AuthUserGateway.BranchMembership::isDefault)
                .map(AuthUserGateway.BranchMembership::branchId)
                .findFirst()
                .orElse(user.branches().get(0).branchId());
    }

    private Duration remainingTtl(JwtClaims claims) {
        if (claims.expiresAt() == null) {
            return Duration.ofHours(1);
        }
        Duration ttl = Duration.between(Instant.now(), claims.expiresAt());
        return ttl.isNegative() ? Duration.ofMinutes(1) : ttl;
    }
}
