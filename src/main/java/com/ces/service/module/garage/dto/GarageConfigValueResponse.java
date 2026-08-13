package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.GarageConfigValue;
import com.ces.service.module.garage.enums.GarageConfigListType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarageConfigValueResponse {

    private UUID id;
    private GarageConfigListType listType;
    private String value;
    private Boolean isRequired;
    private Boolean defaultUsesEngineHours;
    private Boolean defaultUsesKm;
    private Boolean isSystem;
    private Boolean isActive;
    private Integer sortOrder;

    public static GarageConfigValueResponse from(GarageConfigValue v) {
        return GarageConfigValueResponse.builder()
                .id(v.getId())
                .listType(v.getListType())
                .value(v.getValue())
                .isRequired(v.getIsRequired())
                .defaultUsesEngineHours(v.getDefaultUsesEngineHours())
                .defaultUsesKm(v.getDefaultUsesKm())
                .isSystem(v.getIsSystem())
                .isActive(v.getIsActive())
                .sortOrder(v.getSortOrder())
                .build();
    }
}
