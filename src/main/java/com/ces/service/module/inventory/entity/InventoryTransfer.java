package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.inventory.enums.TransferStatus;
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
 * A move of stock between two folders, in two steps.
 *
 * <p>Quantity leaves the source when the transfer is sent and only reaches the destination when
 * somebody receives it. The gap is real — a trolley takes time to cross a warehouse — and holding
 * it here means a count taken mid-move can still be explained.
 */
@Entity
@Table(name = "inventory_transfers", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventoryTransfer extends BaseEntity {

    @Column(name = "from_node_id", nullable = false)
    private UUID fromNodeId;

    @Column(name = "to_node_id", nullable = false)
    private UUID toNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TransferStatus status = TransferStatus.IN_TRANSIT;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** Separate from {@code createdBy} because receiving is its own act by its own person. */
    @Column(name = "sent_by")
    private UUID sentBy;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private Instant sentAt = Instant.now();

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
