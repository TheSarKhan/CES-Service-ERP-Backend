package com.ces.service.module.enginehours.dto;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Manual completion of a maintenance plan line. Meter values are optional — omit whichever the
 * vehicle doesn't track — but when given must not be below the vehicle's current cached value;
 * they're recorded as a normal {@code MeterReading} (source "Baxım") in the same transaction.
 *
 * <p>{@code materials} is the structured, Anbar-linked list (see {@link MaterialLineRequest});
 * {@code materialsNotes} stays alongside it as a free-text note for anything not worth tracking
 * as a real stock movement — the two are additive, not a replacement of one by the other.
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
    /** Optional — omitted or empty means no structured materials, only {@code materialsNotes}. */
    @Valid
    private List<MaterialLineRequest> materials;
}
