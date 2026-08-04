package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** One product on a counting sheet. */
@Entity
@Table(name = "inventory_stocktake_lines", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StocktakeLine extends BaseEntity {

    @Column(name = "stocktake_id", nullable = false)
    private UUID stocktakeId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    /**
     * Frozen when the sheet was opened. Without freezing it, someone else's stock-in mid-count
     * would move the variance and produce a number nobody actually counted.
     */
    @Column(name = "system_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal systemQuantity;

    /** Null means not counted yet — which is not the same as counted and found to be zero. */
    @Column(name = "counted_quantity", precision = 12, scale = 3)
    private BigDecimal countedQuantity;

    @Column(name = "counted_by")
    private UUID countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
