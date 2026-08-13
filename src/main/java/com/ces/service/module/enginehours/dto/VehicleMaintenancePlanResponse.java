package com.ces.service.module.enginehours.dto;

import com.ces.service.module.enginehours.entity.VehicleMaintenancePlan;
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
public class VehicleMaintenancePlanResponse {

    private UUID id;
    private UUID vehicleId;
    private String maintenanceType;
    private BigDecimal intervalMeterHours;
    private BigDecimal intervalKm;
    private Integer intervalCalendarDays;
    private BigDecimal lastDoneEngineHours;
    private BigDecimal lastDoneKm;
    private LocalDate lastDoneDate;
    private BigDecimal nextDueEngineHours;
    private BigDecimal nextDueKm;
    private LocalDate nextDueDate;
    private Boolean isActive;
    private String notes;
    /** Whichever configured condition (meter or calendar) has been reached first. */
    private Boolean isDue;
    /** How far past due, in the unit of whichever condition triggered {@code isDue}; null if not due. */
    private BigDecimal overdueMeterAmount;
    private Integer overdueDays;

    public static VehicleMaintenancePlanResponse from(
            VehicleMaintenancePlan p, boolean isDue, BigDecimal overdueMeterAmount, Integer overdueDays) {
        return VehicleMaintenancePlanResponse.builder()
                .id(p.getId())
                .vehicleId(p.getVehicleId())
                .maintenanceType(p.getMaintenanceType())
                .intervalMeterHours(p.getIntervalMeterHours())
                .intervalKm(p.getIntervalKm())
                .intervalCalendarDays(p.getIntervalCalendarDays())
                .lastDoneEngineHours(p.getLastDoneEngineHours())
                .lastDoneKm(p.getLastDoneKm())
                .lastDoneDate(p.getLastDoneDate())
                .nextDueEngineHours(p.getNextDueEngineHours())
                .nextDueKm(p.getNextDueKm())
                .nextDueDate(p.getNextDueDate())
                .isActive(p.getIsActive())
                .notes(p.getNotes())
                .isDue(isDue)
                .overdueMeterAmount(overdueMeterAmount)
                .overdueDays(overdueDays)
                .build();
    }
}
