package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.WarrantyExtension;
import com.ces.service.module.inventory.entity.WarrantyTargetType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One row of warranty extension history. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyExtensionResponse {

    private UUID id;
    private WarrantyTargetType targetType;
    private UUID targetId;
    private String targetLabel;
    private LocalDate previousEndDate;
    private LocalDate newEndDate;
    private Integer monthsAdded;
    private String reason;
    private UUID createdBy;
    private Instant createdAt;

    public static WarrantyExtensionResponse from(WarrantyExtension extension) {
        return WarrantyExtensionResponse.builder()
                .id(extension.getId())
                .targetType(extension.getTargetType())
                .targetId(extension.getTargetId())
                .targetLabel(extension.getTargetLabel())
                .previousEndDate(extension.getPreviousEndDate())
                .newEndDate(extension.getNewEndDate())
                .monthsAdded(extension.getMonthsAdded())
                .reason(extension.getReason())
                .createdBy(extension.getCreatedBy())
                .createdAt(extension.getCreatedAt())
                .build();
    }
}
