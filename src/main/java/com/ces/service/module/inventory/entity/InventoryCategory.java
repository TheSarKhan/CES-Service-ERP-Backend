package com.ces.service.module.inventory.entity;

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

/** Product category (Elektronika / Mebel / Kimyəvi maddələr ...), owning a dynamic field schema. */
@Entity
@Table(name = "inventory_categories", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventoryCategory extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Default unit of measure for products in this category (e.g. "Ədəd", "Litr", "Metr"). */
    @Column(name = "default_unit", nullable = false, length = 50)
    private String defaultUnit;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
