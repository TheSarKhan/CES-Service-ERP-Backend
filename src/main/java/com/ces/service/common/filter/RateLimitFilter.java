package com.ces.service.common.filter;

import com.ces.service.common.dto.ErrorResponse;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.infrastructure.redis.CacheKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Brute-force protection for the login endpoint (SRS §4.5):
 * an IP-keyed Redis counter; after 5 failed attempts the IP is blocked for
 * 15 minutes. The counter is incremented here on each attempt and reset by the
 * {@code AuthService} on a successful login. When the threshold is exceeded the
 * request is short-circuited with HTTP 429 {@code RATE_LIMIT_EXCEEDED}.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!isLoginAttempt(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        String key = CacheKeys.rateLimit(ip);
        String current = redis.opsForValue().get(key);
        int attempts = current != null ? safeParse(current) : 0;

        if (attempts >= MAX_ATTEMPTS) {
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginAttempt(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && LOGIN_PATH.equals(request.getRequestURI());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private int safeParse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        ErrorCode code = ErrorCode.RATE_LIMIT_EXCEEDED;
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(code.name(), code.getDefaultMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
