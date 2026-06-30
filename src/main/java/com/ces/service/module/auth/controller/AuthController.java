package com.ces.service.module.auth.controller;

import com.ces.service.common.dto.ApiResponse;
import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.security.SecurityUtils;
import com.ces.service.module.auth.dto.ForgotPasswordRequest;
import com.ces.service.module.auth.dto.LoginRequest;
import com.ces.service.module.auth.dto.RefreshRequest;
import com.ces.service.module.auth.dto.ResetPasswordRequest;
import com.ces.service.module.auth.dto.SwitchBranchRequest;
import com.ces.service.module.auth.dto.TokenResponse;
import com.ces.service.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Authentication endpoints (SRS §4.2).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, token refresh, branch switching, password reset")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        String token = extractBearer(httpRequest);
        authService.logout(token);
        return ApiResponse.empty();
    }

    @PostMapping("/switch-branch")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TokenResponse> switchBranch(@Valid @RequestBody SwitchBranchRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        return ApiResponse.ok(authService.switchBranch(userId, request));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.empty();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.empty();
    }

    private String extractBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Missing bearer token");
        }
        return header.substring(BEARER_PREFIX.length()).trim();
    }
}
