package com.ces.service.module.garage.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One template line. At least one interval must be set — checked in the service layer with a
 * message pointing at which line is wrong, ahead of the DB CHECK that would otherwise reject the
 * whole batch with no indication of which item failed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GarageMaintenanceTemplateItemRequest {

    @NotBlank
    private String maintenanceType;

    private BigDecimal intervalMeterHours;
    private BigDecimal intervalKm;
    private Integer intervalCalendarDays;
    private String notes;
}
