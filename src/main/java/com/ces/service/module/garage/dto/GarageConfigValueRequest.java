package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.enums.GarageConfigListType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class GarageConfigValueRequest {

    @NotNull
    private GarageConfigListType listType;

    @NotBlank
    private String value;

    private Boolean isRequired;

    /** Only meaningful when listType is EQUIPMENT_TYPE. */
    private Boolean defaultUsesEngineHours;

    private Boolean defaultUsesKm;

    private Boolean isActive;

    private Integer sortOrder;
}
