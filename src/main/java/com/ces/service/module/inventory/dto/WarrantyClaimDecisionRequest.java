package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.enums.WarrantyClaimResolution;
import com.ces.service.module.inventory.enums.WarrantyClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Recording the supplier's answer. {@code status} carries the commercially meaningful part —
 * ACCEPTED means they cover the cost, REJECTED means we do.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyClaimDecisionRequest {

    @NotNull
    private WarrantyClaimStatus status;

    private WarrantyClaimResolution resolution;

    @Size(max = 4000)
    private String decisionNotes;

    /** Defaults to today when omitted. */
    private LocalDate decidedAt;
}
