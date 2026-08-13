package com.ces.service.module.enginehours.dto;

import com.ces.service.module.enginehours.enums.MeterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A deliberate reset — the new value may sit below the vehicle's current cached value (meter
 * replaced, correcting a bad past entry, ...). Always goes through the approval queue with a
 * mandatory reason; a plain {@link MeterReadingRequest} is rejected outright if it would decrease.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterRolloverRequest {

    @NotNull
    private MeterType meterType;

    @NotNull
    private BigDecimal value;

    private LocalDate recordedAt;

    @NotBlank
    private String reason;

    private String notes;
}
