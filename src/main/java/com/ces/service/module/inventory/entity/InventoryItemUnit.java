package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.inventory.enums.InventoryUnitStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
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
 * A single physically-serialized, individually warranty-tracked unit of an {@link InventoryItem}
 * (e.g. one battery out of a batch of 50, each with its own serial number and warranty window).
 */
@Entity
@Table(name = "inventory_item_units", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventoryItemUnit extends BaseEntity {

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "node_id", nullable = false)
    private UUID nodeId;

    @Column(name = "serial_number", nullable = false, length = 255)
    private String serialNumber;

    @Column(name = "qr_code", length = 255)
    private String qrCode;

    @Column(name = "barcode", length = 255)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private InventoryUnitStatus status = InventoryUnitStatus.IN_STOCK;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "failure_notes", columnDefinition = "text")
    private String failureNotes;

    @Column(name = "used_in_work_order_id")
    private UUID usedInWorkOrderId;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
