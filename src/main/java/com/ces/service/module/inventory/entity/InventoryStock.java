package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * How much of one product sits in one folder.
 *
 * <p>Stock used to be a column on {@link InventoryItem}, which forced a product to live in exactly
 * one place. It lives here now so the same product can be held in several locations and still have
 * one honest answer to "how many do we have" — the sum of its rows.
 *
 * <p>For a serialized product this row is derived: the units carry the truth, and the quantity here
 * is recomputed from them. For everything else this row *is* the truth.
 */
@Entity
@Table(name = "inventory_stock", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventoryStock extends BaseEntity {

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;
}
