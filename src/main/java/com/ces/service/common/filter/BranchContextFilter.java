package com.ces.service.common.filter;

import com.ces.service.common.dto.ErrorResponse;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.security.BranchContext;
import com.ces.service.common.security.CesUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads the {@code X-Branch-Id} header, verifies it is among the branches carried
 * by the authenticated principal, and binds it to {@link BranchContext} for the
 * duration of the request (SRS §5.4 / §5.6). The binding is always cleared in a
 * {@code finally} block to prevent thread-local leakage across pooled threads.
 */
@Component
public class BranchContextFilter extends OncePerRequestFilter {

    public static final String BRANCH_HEADER = "X-Branch-Id";

    private final ObjectMapper objectMapper;

    public BranchContextFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.getPrincipal() instanceof CesUserPrincipal;

        if (!authenticated) {
            // Unauthenticated (public) endpoints have no branch scope.
            filterChain.doFilter(request, response);
            return;
        }

        CesUserPrincipal principal = (CesUserPrincipal) auth.getPrincipal();
        String headerValue = request.getHeader(BRANCH_HEADER);

        UUID branchId;
        if (headerValue != null && !headerValue.isBlank()) {
            try {
                branchId = UUID.fromString(headerValue.trim());
            } catch (IllegalArgumentException e) {
                writeError(response, ErrorCode.BRANCH_ACCESS_DENIED);
                return;
            }
            // The header must reference a branch the token is allowed to act in.
            boolean allowed = branchId.equals(principal.branchId()) || principal.canAccessBranch(branchId);
            if (!allowed) {
                writeError(response, ErrorCode.BRANCH_ACCESS_DENIED);
                return;
            }
        } else {
            // Default to the active branch carried by the token.
            branchId = principal.branchId();
        }

        try {
            if (branchId != null) {
                BranchContext.set(branchId);
            }
            filterChain.doFilter(request, response);
        } finally {
            BranchContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(code.name(), code.getDefaultMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
