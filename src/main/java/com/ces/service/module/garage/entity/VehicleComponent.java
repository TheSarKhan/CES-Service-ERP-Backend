package com.ces.service.module.garage.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.garage.enums.VehicleComponentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
 * One installed (or once-installed) component. There is no separate history table: replacing a
 * component flips this row to REMOVED and a fresh ACTIVE row is inserted for its successor, so
 * "the component history" is simply every row for the vehicle — the same lifecycle-on-the-row
 * pattern {@code InventoryItemUnit} uses.
 */
@Entity
@Table(name = "vehicle_components", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VehicleComponent extends BaseEntity {

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    /** Free text — validated and auto-registered against {@code garage_config_values(COMPONENT_TYPE)}. */
    @Column(name = "component_type", nullable = false)
    private String componentType;

    @Column(name = "identifier")
    private String identifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private VehicleComponentStatus status = VehicleComponentStatus.ACTIVE;

    @Column(name = "installed_at", nullable = false)
    private LocalDate installedAt;

    @Column(name = "installed_meter_value")
    private BigDecimal installedMeterValue;

    @Column(name = "removed_at")
    private LocalDate removedAt;

    @Column(name = "removed_meter_value")
    private BigDecimal removedMeterValue;

    @Column(name = "removal_reason")
    private String removalReason;

    @Column(name = "notes")
    private String notes;
}
