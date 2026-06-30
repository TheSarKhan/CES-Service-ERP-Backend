package com.ces.service.module.role.dto;

import com.ces.service.module.role.entity.Permission;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Single permission catalog entry. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String module;
    private String permType;
    private String httpMethod;
    private Boolean isActive;

    public static PermissionResponse from(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .module(p.getModule())
                .permType(p.getPermType())
                .httpMethod(p.getHttpMethod())
                .isActive(p.getIsActive())
                .build();
    }
}
