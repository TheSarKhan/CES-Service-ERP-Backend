package com.ces.service.module.enginehours.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Manual completion of a maintenance plan line. Meter values are optional — omit whichever the
 * vehicle doesn't track — but when given must not be below the vehicle's current cached value;
 * they're recorded as a normal {@code MeterReading} (source "Baxım") in the same transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMaintenanceCompletionRequest {

    private LocalDate completedAt;
    private BigDecimal meterEngineHours;
    private BigDecimal meterKm;
    private String description;
    private String materialsNotes;
    private String notes;
}
