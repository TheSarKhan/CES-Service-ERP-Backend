package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.WarrantyTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** "Zəmanət tələbi aç" — filing a claim with the supplier for a failed item or unit. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyClaimRequest {

    @NotNull
    private WarrantyTargetType targetType;

    @NotNull
    private UUID targetId;

    /**
     * Left open rather than copied blindly from the product: a claim can go to a different
     * supplier than the one currently on the record. Defaults to the product's supplier.
     */
    @Size(max = 255)
    private String supplier;

    @Size(max = 100)
    private String claimNumber;

    @Size(max = 4000)
    private String description;

    /** Defaults to today when omitted. */
    private LocalDate submittedAt;
}
