package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.GarageMaintenanceTemplateItem;
import java.math.BigDecimal;
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
public class GarageMaintenanceTemplateItemResponse {

    private UUID id;
    private String maintenanceType;
    private BigDecimal intervalMeterHours;
    private BigDecimal intervalKm;
    private Integer intervalCalendarDays;
    private String notes;

    public static GarageMaintenanceTemplateItemResponse from(GarageMaintenanceTemplateItem i) {
        return GarageMaintenanceTemplateItemResponse.builder()
                .id(i.getId())
                .maintenanceType(i.getMaintenanceType())
                .intervalMeterHours(i.getIntervalMeterHours())
                .intervalKm(i.getIntervalKm())
                .intervalCalendarDays(i.getIntervalCalendarDays())
                .notes(i.getNotes())
                .build();
    }
}
