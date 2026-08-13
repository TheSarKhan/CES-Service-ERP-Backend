package com.ces.service.module.garage.entity;

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
 * One maintenance line inside a template (e.g. "Yağ dəyişimi — 1000 saat"). At least one of the
 * three intervals must be set — enforced by a DB CHECK, since "whichever comes first" needs at
 * least one condition to compare.
 */
@Entity
@Table(name = "garage_maintenance_template_items", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GarageMaintenanceTemplateItem extends BaseEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    /** Free text — validated and auto-registered against {@code garage_config_values(MAINTENANCE_TYPE)}. */
    @Column(name = "maintenance_type", nullable = false)
    private String maintenanceType;

    @Column(name = "interval_meter_hours")
    private BigDecimal intervalMeterHours;

    @Column(name = "interval_km")
    private BigDecimal intervalKm;

    @Column(name = "interval_calendar_days")
    private Integer intervalCalendarDays;

    @Column(name = "notes")
    private String notes;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
