package com.ces.service.module.user.dto;

import com.ces.service.module.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Summary user view (list rows). Never exposes the password hash. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private UUID branchId;
    private String fullName;
    private String email;
    private String phone;
    private String position;
    private Boolean isActive;
    private Instant lastLoginAt;
    private Instant createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .branchId(user.getBranchId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .position(user.getPosition())
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
