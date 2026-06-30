package com.ces.service.module.user.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Last-login + activity summary (GET /users/{id}/activity). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityResponse {

    private UUID userId;
    private String email;
    private Instant lastLoginAt;
    private Integer failedAttempts;
    private Instant lockedUntil;
    private Boolean isLocked;
    private Boolean isActive;
}
