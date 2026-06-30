package com.ces.service.config;

import com.ces.service.common.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA auditing wiring. Supplies the {@link AuditorAware} used to populate
 * {@code created_by} / {@code updated_by} on {@code BaseEntity}, resolving the
 * current user from the security context and falling back to a configured system
 * UUID when there is no authenticated principal (e.g. Flyway-seeded data,
 * scheduled jobs).
 */
@Configuration
public class JpaConfig {

    private final UUID systemUserId;

    public JpaConfig(@Value("${ces.security.system-user-id}") String systemUserId) {
        this.systemUserId = UUID.fromString(systemUserId);
    }

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> Optional.of(SecurityUtils.getCurrentUserId().orElse(systemUserId));
    }
}
