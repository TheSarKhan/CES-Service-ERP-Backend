package com.ces.service.module.garage.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.garage.enums.GarageConfigListType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * One value in one of Qaraj's manageable dropdown lists. See {@link GarageConfigListType} for why
 * this single table covers every list instead of a per-list table or a full EAV system.
 */
@Entity
@Table(name = "garage_config_values", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GarageConfigValue extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false)
    private GarageConfigListType listType;

    @Column(name = "value", nullable = false)
    private String value;

    /** Meaningful for DOC_TYPE / PHOTO_CATEGORY: a soft "this one matters" flag, not a hard gate. */
    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = Boolean.FALSE;

    /** Meaningful only for EQUIPMENT_TYPE rows — the starting point a new vehicle of this type copies. */
    @Column(name = "default_uses_engine_hours")
    private Boolean defaultUsesEngineHours;

    @Column(name = "default_uses_km")
    private Boolean defaultUsesKm;

    /** STATUS and METER_SOURCE rows other code names by exact string — protected from deletion. */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = Boolean.FALSE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
