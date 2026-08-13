package com.ces.service.module.garage.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A named maintenance template for one equipment type (e.g. "Ekskavator baxım şablonu").
 * Configuration data only — applying a template to a vehicle, and tracking that vehicle's actual
 * due dates, is the Motosaat module's job, not this one's.
 */
@Entity
@Table(name = "garage_maintenance_templates", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GarageMaintenanceTemplate extends BaseEntity {

    @Column(name = "equipment_type", nullable = false)
    private String equipmentType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = Boolean.TRUE;
}
