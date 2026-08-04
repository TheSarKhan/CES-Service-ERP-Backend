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

/** One product on a transfer. Several fit on one, so emptying a shelf isn't one form per item. */
@Entity
@Table(name = "inventory_transfer_lines", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventoryTransferLine extends BaseEntity {

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
}
