package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A single node in the dynamic physical storage tree (Layer), e.g. Anbar → Blok → Otaq → Şkaf.
 *
 * <p>Adjacency list only — {@code parent_id} is the sole structural pointer, no depth/path is
 * persisted (modelled after a proven Folder-tree pattern: compute shape on read, not on write).
 * Depth is unbounded. Products may only be placed on leaf nodes (enforced in the service layer,
 * since "leaf" isn't something a simple column constraint can express).
 */
@Entity
@Table(name = "inventory_nodes", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventoryNode extends BaseEntity {

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "qr_code", length = 255)
    private String qrCode;

    @Column(name = "barcode", length = 255)
    private String barcode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /**
     * Optional restriction on which categories may be used for items placed at this node. Empty
     * = unrestricted (any category is allowed) — see {@code inventory_node_categories} migration.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "inventory_node_categories",
            schema = "ces_service",
            joinColumns = @JoinColumn(name = "node_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @Builder.Default
    private Set<InventoryCategory> allowedCategories = new HashSet<>();
}
