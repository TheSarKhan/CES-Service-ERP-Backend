package com.ces.service.module.enginehours.dto;

import com.ces.service.module.enginehours.entity.VehicleMaintenanceCompletion;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class VehicleMaintenanceCompletionResponse {

    private UUID id;
    private UUID planId;
    /** Denormalized off the plan at read time, so the frontend needn't join the two lists itself. */
    private String maintenanceType;
    private UUID vehicleId;
    private LocalDate completedAt;
    private BigDecimal meterEngineHours;
    private BigDecimal meterKm;
    private String description;
    private String materialsNotes;
    private String notes;

    public static VehicleMaintenanceCompletionResponse from(VehicleMaintenanceCompletion c, String maintenanceType) {
        return VehicleMaintenanceCompletionResponse.builder()
                .id(c.getId())
                .planId(c.getPlanId())
                .maintenanceType(maintenanceType)
                .vehicleId(c.getVehicleId())
                .completedAt(c.getCompletedAt())
                .meterEngineHours(c.getMeterEngineHours())
                .meterKm(c.getMeterKm())
                .description(c.getDescription())
                .materialsNotes(c.getMaterialsNotes())
                .notes(c.getNotes())
                .build();
    }
}
