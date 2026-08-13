package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.VehicleComponent;
import com.ces.service.module.garage.enums.VehicleComponentStatus;
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
public class VehicleComponentResponse {

    private UUID id;
    private UUID vehicleId;
    private String componentType;
    private String identifier;
    private VehicleComponentStatus status;
    private LocalDate installedAt;
    private BigDecimal installedMeterValue;
    private LocalDate removedAt;
    private BigDecimal removedMeterValue;
    private String removalReason;
    private String notes;

    public static VehicleComponentResponse from(VehicleComponent c) {
        return VehicleComponentResponse.builder()
                .id(c.getId())
                .vehicleId(c.getVehicleId())
                .componentType(c.getComponentType())
                .identifier(c.getIdentifier())
                .status(c.getStatus())
                .installedAt(c.getInstalledAt())
                .installedMeterValue(c.getInstalledMeterValue())
                .removedAt(c.getRemovedAt())
                .removedMeterValue(c.getRemovedMeterValue())
                .removalReason(c.getRemovalReason())
                .notes(c.getNotes())
                .build();
    }
}
