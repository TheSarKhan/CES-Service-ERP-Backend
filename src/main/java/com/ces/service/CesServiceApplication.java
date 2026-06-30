package com.ces.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the CES Service Management System backend.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableFeignClients(basePackages = "com.ces.service")
@EnableScheduling
@EnableCaching
public class CesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CesServiceApplication.class, args);
    }
}
