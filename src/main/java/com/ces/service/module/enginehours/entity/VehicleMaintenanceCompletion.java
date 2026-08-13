package com.ces.service.module.enginehours.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * One "Baxımı tamamla" event against a {@link VehicleMaintenancePlan}. Manual-only for now — the
 * "Servis vasitəsilə" completion path (brief) is added once the Service module exists to source
 * it from.
 */
@Entity
@Table(name = "vehicle_maintenance_completions", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VehicleMaintenanceCompletion extends BaseEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "completed_at", nullable = false)
    private LocalDate completedAt;

    @Column(name = "meter_engine_hours")
    private BigDecimal meterEngineHours;

    @Column(name = "meter_km")
    private BigDecimal meterKm;

    @Column(name = "description")
    private String description;

    @Column(name = "materials_notes")
    private String materialsNotes;

    @Column(name = "notes")
    private String notes;
}
