package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.inventory.enums.StockMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * One line of the stock ledger.
 *
 * <p>Rows are never edited or deleted: a mistaken movement is closed with an opposing movement, the
 * way a ledger works. That is what makes "where did these 50 come from" answerable months later —
 * and it is also what count corrections and relocations are built on.
 */
@Entity
@Table(name = "inventory_stock_movements", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StockMovement extends BaseEntity {

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    /** Set when a serialized unit moved; null for quantity-tracked products. */
    @Column(name = "unit_id")
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType movementType;

    /** Signed: positive brings stock in, negative takes it out. */
    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    /**
     * The location's quantity immediately after this movement. Stored rather than derived so a
     * history row still reads correctly no matter what happened afterwards.
     */
    @Column(name = "balance_after", nullable = false, precision = 12, scale = 3)
    private BigDecimal balanceAfter;

    /** What caused it: APPROVAL, ITEM_MOVE, UNIT — free-form so new sources fit. */
    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;
}
