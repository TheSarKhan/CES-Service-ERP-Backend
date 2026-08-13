package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.GarageMaintenanceTemplate;
import java.util.List;
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
public class GarageMaintenanceTemplateResponse {

    private UUID id;
    private String equipmentType;
    private String name;
    private Boolean isActive;
    private List<GarageMaintenanceTemplateItemResponse> items;

    public static GarageMaintenanceTemplateResponse from(
            GarageMaintenanceTemplate t, List<GarageMaintenanceTemplateItemResponse> items) {
        return GarageMaintenanceTemplateResponse.builder()
                .id(t.getId())
                .equipmentType(t.getEquipmentType())
                .name(t.getName())
                .isActive(t.getIsActive())
                .items(items)
                .build();
    }
}
