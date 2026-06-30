package com.ces.service.common.filter;

import com.ces.service.common.dto.ErrorResponse;
import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.security.CesUserPrincipal;
import com.ces.service.infrastructure.redis.RedisTokenStore;
import com.ces.service.module.auth.service.JwtClaims;
import com.ces.service.module.auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Parses the {@code Authorization: Bearer} header, validates the JWT, checks the
 * Redis blacklist by {@code jti}, and installs a {@link CesUserPrincipal} into the
 * {@code SecurityContext} with the permission claims as granted authorities
 * (SRS §4.3).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final RedisTokenStore tokenStore;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   RedisTokenStore tokenStore,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            JwtClaims claims = jwtService.parse(token);

            if (tokenStore.isBlacklisted(claims.jti())) {
                writeError(response, ErrorCode.AUTH_TOKEN_REVOKED);
                return;
            }

            List<SimpleGrantedAuthority> authorities = claims.permissions().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            // The branch list is derived from the active branch on the token; the
            // full branch list is re-validated by BranchContextFilter against the
            // X-Branch-Id header where applicable. We seed it with the active one.
            List<UUID> branches = claims.branchId() != null ? List.of(claims.branchId()) : List.of();

            CesUserPrincipal principal = new CesUserPrincipal(
                    claims.subject(),
                    claims.email(),
                    claims.branchId(),
                    branches,
                    claims.roles(),
                    claims.permissions()
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (BusinessException e) {
            writeError(response, e.getErrorCode());
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(code.name(), code.getDefaultMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
