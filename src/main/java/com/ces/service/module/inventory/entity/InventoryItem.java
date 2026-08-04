package com.ces.service.module.inventory.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A product (Məhsul) — the catalogue entry: what it is, not where it is.
 *
 * <p>Location and quantity live in {@link InventoryStock}, one row per folder holding it, because
 * the same product is routinely kept in more than one place. Its total is the sum of those rows.
 * For a serialized product the {@link InventoryItemUnit} rows are the truth and the stock rows are
 * recomputed from them.
 *
 * <p>{@code attributes} holds the dynamic field values defined by its {@link InventoryCategory}'s
 * field schema, keyed by {@code fieldKey}.
 */
@Entity
@Table(name = "inventory_items", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventoryItem extends BaseEntity {

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "barcode", length = 255)
    private String barcode;

    @Column(name = "qr_code", length = 255)
    private String qrCode;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "purchase_price", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(name = "is_serialized", nullable = false)
    @Builder.Default
    private Boolean isSerialized = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String attributes = "{}";

    /**
     * Warranty duration in months. For a serialized item this is the default applied to newly
     * registered units; for a non-serialized one it's the length of {@code warrantyStartDate} →
     * {@code warrantyEndDate}.
     */
    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    /**
     * Only meaningful for non-serialized items — a serialized item's real warranty lives on each
     * {@link InventoryItemUnit}, so this stays null there to avoid two competing answers.
     */
    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    /**
     * Reorder point, compared against the total across every folder. Null means nobody tracks a
     * level for this product, so it never raises a warning.
     */
    @Column(name = "min_quantity", precision = 12, scale = 3)
    private BigDecimal minQuantity;

    /** Below this, work stops. Meant to sit under {@code minQuantity}. */
    @Column(name = "critical_quantity", precision = 12, scale = 3)
    private BigDecimal criticalQuantity;

    /**
     * Who the warranty claim goes to. A real column rather than a dynamic category attribute: it's
     * filtered and grouped by on the warranty screen, and JSONB can do neither cheaply.
     */
    @Column(name = "supplier", length = 255)
    private String supplier;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
