package com.ces.service.module.enginehours.dto;

import com.ces.service.module.enginehours.entity.MeterReading;
import com.ces.service.module.enginehours.enums.MeterType;
import java.math.BigDecimal;
import java.time.Instant;
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
public class MeterReadingResponse {

    private UUID id;
    private UUID vehicleId;
    private MeterType meterType;
    private BigDecimal value;
    private BigDecimal previousValue;
    private BigDecimal delta;
    private String source;
    private Boolean isRollover;
    private String rolloverReason;
    private UUID sourceRefId;
    private LocalDate recordedAt;
    private String notes;
    private Instant createdAt;

    public static MeterReadingResponse from(MeterReading r) {
        return MeterReadingResponse.builder()
                .id(r.getId())
                .vehicleId(r.getVehicleId())
                .meterType(r.getMeterType())
                .value(r.getValue())
                .previousValue(r.getPreviousValue())
                .delta(r.getDelta())
                .source(r.getSource())
                .isRollover(r.getIsRollover())
                .rolloverReason(r.getRolloverReason())
                .sourceRefId(r.getSourceRefId())
                .recordedAt(r.getRecordedAt())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
