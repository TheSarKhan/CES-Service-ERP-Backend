package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.WarrantyClaim;
import com.ces.service.module.inventory.entity.WarrantyTargetType;
import com.ces.service.module.inventory.enums.WarrantyClaimResolution;
import com.ces.service.module.inventory.enums.WarrantyClaimStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One warranty claim. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyClaimResponse {

    private UUID id;
    private WarrantyTargetType targetType;
    private UUID targetId;
    private String targetLabel;
    private UUID itemId;
    private String supplier;
    private String claimNumber;
    private WarrantyClaimStatus status;
    private WarrantyClaimResolution resolution;
    private String description;
    private String decisionNotes;
    private LocalDate submittedAt;
    private LocalDate decidedAt;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static WarrantyClaimResponse from(WarrantyClaim claim) {
        return WarrantyClaimResponse.builder()
                .id(claim.getId())
                .targetType(claim.getTargetType())
                .targetId(claim.getTargetId())
                .targetLabel(claim.getTargetLabel())
                .itemId(claim.getItemId())
                .supplier(claim.getSupplier())
                .claimNumber(claim.getClaimNumber())
                .status(claim.getStatus())
                .resolution(claim.getResolution())
                .description(claim.getDescription())
                .decisionNotes(claim.getDecisionNotes())
                .submittedAt(claim.getSubmittedAt())
                .decidedAt(claim.getDecidedAt())
                .createdBy(claim.getCreatedBy())
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }
}
