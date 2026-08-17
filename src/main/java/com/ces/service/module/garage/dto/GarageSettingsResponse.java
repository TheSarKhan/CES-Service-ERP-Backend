package com.ces.service.module.garage.dto;

import com.ces.service.module.garage.entity.GarageSettings;
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
public class GarageSettingsResponse {

    private Integer staleReadingDays;
    private BigDecimal maxNormalIncreaseEngineHours;
    private BigDecimal maxNormalIncreaseKm;

    public static GarageSettingsResponse from(GarageSettings s) {
        return GarageSettingsResponse.builder()
                .staleReadingDays(s.getStaleReadingDays())
                .maxNormalIncreaseEngineHours(s.getMaxNormalIncreaseEngineHours())
                .maxNormalIncreaseKm(s.getMaxNormalIncreaseKm())
                .build();
    }

    /** No row saved yet for this branch — every threshold reads as "off". */
    public static GarageSettingsResponse empty() {
        return GarageSettingsResponse.builder().build();
    }
}
