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
 * One maintenance line tracked against a specific vehicle — either cloned from a
 * {@code GarageMaintenanceTemplateItem} when a template is applied, or added directly. Later
 * changes to the source template never propagate here (brief): each plan owns its own copy of
 * the interval.
 */
@Entity
@Table(name = "vehicle_maintenance_plans", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class VehicleMaintenancePlan extends BaseEntity {

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    /** Free text — validated and auto-registered against {@code garage_config_values(MAINTENANCE_TYPE)}. */
    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType;

    @Column(name = "interval_meter_hours")
    private BigDecimal intervalMeterHours;

    @Column(name = "interval_km")
    private BigDecimal intervalKm;

    @Column(name = "interval_calendar_days")
    private Integer intervalCalendarDays;

    @Column(name = "last_done_engine_hours")
    private BigDecimal lastDoneEngineHours;

    @Column(name = "last_done_km")
    private BigDecimal lastDoneKm;

    @Column(name = "last_done_date")
    private LocalDate lastDoneDate;

    @Column(name = "next_due_engine_hours")
    private BigDecimal nextDueEngineHours;

    @Column(name = "next_due_km")
    private BigDecimal nextDueKm;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "source_template_item_id")
    private UUID sourceTemplateItemId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notes")
    private String notes;
}
