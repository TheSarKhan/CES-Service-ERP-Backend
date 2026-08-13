package com.ces.service.module.enginehours.dto;

import com.ces.service.module.enginehours.enums.MeterType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A normal forward reading. {@code source} defaults to "Manual" when left blank. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReadingRequest {

    @NotNull
    private MeterType meterType;

    @NotNull
    private BigDecimal value;

    private LocalDate recordedAt;
    private String source;
    private String notes;
}
