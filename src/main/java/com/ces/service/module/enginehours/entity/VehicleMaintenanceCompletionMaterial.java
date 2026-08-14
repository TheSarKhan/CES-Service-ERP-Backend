package com.ces.service.module.enginehours.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.enginehours.enums.MaterialKind;
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
 * One material line on a {@link VehicleMaintenanceCompletion} — either a quantity-tracked
 * consumable (submits an Anbar stock-out) or a specific serialized unit (marked IN_USE and
 * mirrored into {@code vehicle_components}). See the migration header for the full split.
 *
 * <p>{@code inventoryItemName}/{@code unit}/{@code serialNumber} are snapshotted at write time so
 * this list keeps reading correctly even if the source Inventory row is later renamed or deleted.
 */
@Entity
@Table(name = "vehicle_maintenance_completion_materials", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VehicleMaintenanceCompletionMaterial extends BaseEntity {

    @Column(name = "completion_id", nullable = false)
    private UUID completionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private MaterialKind kind;

    @Column(name = "inventory_item_id")
    private UUID inventoryItemId;

    @Column(name = "inventory_item_name")
    private String inventoryItemName;

    @Column(name = "inventory_node_id")
    private UUID inventoryNodeId;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "stock_approval_request_id")
    private UUID stockApprovalRequestId;

    @Column(name = "inventory_unit_id")
    private UUID inventoryUnitId;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "vehicle_component_id")
    private UUID vehicleComponentId;

    @Column(name = "notes")
    private String notes;
}
