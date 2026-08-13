package com.ces.service.module.garage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Replaces the template's items wholesale on update — simpler than diffing, and templates are small. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarageMaintenanceTemplateRequest {

    @NotBlank
    private String equipmentType;

    @NotBlank
    private String name;

    private Boolean isActive;

    @NotEmpty
    @Valid
    private List<GarageMaintenanceTemplateItemRequest> items;
}
