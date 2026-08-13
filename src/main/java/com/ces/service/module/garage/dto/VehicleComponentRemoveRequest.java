package com.ces.service.module.garage.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class VehicleComponentRemoveRequest {

    private LocalDate removedAt;
    private BigDecimal removedMeterValue;

    @NotBlank
    private String reason;
}
