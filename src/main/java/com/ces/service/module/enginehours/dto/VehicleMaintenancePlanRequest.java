package com.ces.service.module.enginehours.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    /**
     * Baseline override for a brand-new plan — the "next due" is measured from here instead of
     * the vehicle's current reading, for equipment whose last real service already happened at a
     * known, different value (see {@code VehicleMaintenancePlanService.seedBaseline}). Ignored on
     * update: once a plan exists, only a real completion moves its progress.
     */
    private BigDecimal lastDoneEngineHours;
    private BigDecimal lastDoneKm;
    private LocalDate lastDoneDate;
}
