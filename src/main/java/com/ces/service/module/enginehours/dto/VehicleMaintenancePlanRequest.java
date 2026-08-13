package com.ces.service.module.enginehours.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Add or edit one line of a vehicle's own maintenance plan. At least one interval is required. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMaintenancePlanRequest {

    @NotBlank
    private String maintenanceType;

    private BigDecimal intervalMeterHours;
    private BigDecimal intervalKm;
    private Integer intervalCalendarDays;
    private Boolean isActive;
    private String notes;
}
