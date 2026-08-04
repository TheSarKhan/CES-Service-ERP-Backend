package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.inventory.enums.StocktakeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A blind count of one folder.
 *
 * <p>Blind because the alternative doesn't work: shown the system figure, people confirm it rather
 * than count. The whole exercise exists to catch the cases where the two differ, so the recorded
 * number has to be hidden until the sheet is closed.
 */
@Entity
@Table(name = "inventory_stocktakes", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Stocktake extends BaseEntity {

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StocktakeStatus status = StocktakeStatus.OPEN;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** The single approval covering every variance on the sheet. */
    @Column(name = "approval_request_id")
    private UUID approvalRequestId;

    @Column(name = "opened_by")
    private UUID openedBy;

    @Column(name = "opened_at", nullable = false)
    @Builder.Default
    private Instant openedAt = Instant.now();

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;
}
