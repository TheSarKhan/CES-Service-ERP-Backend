package com.ces.service.config;

import com.ces.service.common.dto.ErrorResponse;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.filter.BranchContextFilter;
import com.ces.service.common.filter.JwtAuthenticationFilter;
import com.ces.service.common.filter.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Stateless JWT security configuration (SRS §4.3).
 *
 * <p>Filter ordering: CORS → RateLimit → JwtAuth → BranchContext → authorization.
 * Method-level security ({@code @PreAuthorize}) is enabled. Public endpoints:
 * the unauthenticated auth endpoints, actuator health, and the Swagger UI.</p>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BranchContextFilter branchContextFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          BranchContextFilter branchContextFilter,
                          RateLimitFilter rateLimitFilter,
                          CorsConfigurationSource corsConfigurationSource,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.branchContextFilter = branchContextFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, ErrorCode.AUTH_INVALID_CREDENTIALS))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, ErrorCode.PERMISSION_DENIED)))
                // Effective order: CORS (Spring-managed) → RateLimit → JwtAuth →
                // BranchContext → AuthorizationFilter. Each custom filter is placed
                // immediately before AuthorizationFilter in reverse insertion order,
                // which yields RateLimit, then JwtAuth, then BranchContext.
                .addFilterBefore(branchContextFilter, AuthorizationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, BranchContextFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Prevent the @Component filters from also being auto-registered in the
    //    servlet container (which would execute them twice). They run only inside
    //    the Spring Security filter chain configured above.

    @Bean
    public FilterRegistrationBean<RateLimitFilter> disableRateLimitFilterAutoReg(RateLimitFilter filter) {
        return disable(filter);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoReg(JwtAuthenticationFilter filter) {
        return disable(filter);
    }

    @Bean
    public FilterRegistrationBean<BranchContextFilter> disableBranchFilterAutoReg(BranchContextFilter filter) {
        return disable(filter);
    }

    private <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disable(T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, ErrorCode code)
            throws java.io.IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(code.name(), code.getDefaultMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
