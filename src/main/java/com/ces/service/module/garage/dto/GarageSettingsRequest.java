package com.ces.service.module.garage.dto;

import java.math.BigDecimal;
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
public class GarageSettingsRequest {

    private Integer staleReadingDays;
    private BigDecimal maxNormalIncreaseEngineHours;
    private BigDecimal maxNormalIncreaseKm;
}
