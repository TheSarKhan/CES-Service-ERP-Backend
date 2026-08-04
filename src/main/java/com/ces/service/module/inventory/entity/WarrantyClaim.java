package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.inventory.enums.WarrantyClaimResolution;
import com.ces.service.module.inventory.enums.WarrantyClaimStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A warranty claim filed with a supplier.
 *
 * <p>Recording the failure told us something broke; this records what we did about it and who
 * ended up paying — the question the whole warranty module exists to answer. Without it a unit can
 * sit at "Sıradan çıxıb" forever with no trace of whether anyone ever chased the supplier.
 *
 * <p>Deliberately not routed through the approval queue: this documents an external event that
 * already happened rather than mutating stock or a warranty window, so making a second person
 * confirm it would only delay the record behind the fact.
 */
@Entity
@Table(name = "warranty_claims", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WarrantyClaim extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private WarrantyTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    /** Serial number or product name at claim time — survives the target being renamed. */
    @Column(name = "target_label", length = 255)
    private String targetLabel;

    /** Set for unit claims too, so every claim can be traced back to a product. */
    @Column(name = "item_id")
    private UUID itemId;

    /** Snapshot: the supplier on the product may change later, the claim went to this one. */
    @Column(name = "supplier", length = 255)
    private String supplier;

    /** The supplier's own reference for this claim, when they give one. */
    @Column(name = "claim_number", length = 100)
    private String claimNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WarrantyClaimStatus status = WarrantyClaimStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = 20)
    private WarrantyClaimResolution resolution;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "decision_notes", columnDefinition = "text")
    private String decisionNotes;

    @Column(name = "submitted_at", nullable = false)
    private LocalDate submittedAt;

    @Column(name = "decided_at")
    private LocalDate decidedAt;
}
