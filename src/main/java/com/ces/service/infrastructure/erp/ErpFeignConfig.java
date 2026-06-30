package com.ces.service.infrastructure.erp;

import feign.RequestInterceptor;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign configuration for {@link ErpApiClient}. Attaches the configured ERP API
 * key on every request and disables Feign's built-in retry (retries are handled
 * by the Redis-backed retry scheduler described in SRS §M13.5).
 *
 * <p>Note: this class is referenced by {@code @FeignClient(configuration=...)};
 * it is intentionally not annotated with {@code @Configuration} so its beans are
 * scoped to the client rather than added to the application context.</p>
 */
public class ErpFeignConfig {

    private final String apiKey;

    public ErpFeignConfig(@Value("${ces.erp.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Bean
    public RequestInterceptor erpApiKeyInterceptor() {
        return template -> {
            if (apiKey != null && !apiKey.isBlank()) {
                template.header("X-Api-Key", apiKey);
            }
        };
    }

    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
