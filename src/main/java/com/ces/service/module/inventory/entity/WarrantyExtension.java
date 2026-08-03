package com.ces.service.module.inventory.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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
 * An applied warranty extension.
 *
 * <p>Written only after the extension is approved, so every row here is a change that actually
 * took effect — the queue holds the requests, this holds the history. Extending a warranty has
 * financial consequences, so "who moved this date, from what, to what, and why" stays answerable.
 */
@Entity
@Table(name = "warranty_extensions", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WarrantyExtension extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private WarrantyTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "target_label", length = 255)
    private String targetLabel;

    /** Null when the record had no warranty at all before this extension. */
    @Column(name = "previous_end_date")
    private LocalDate previousEndDate;

    @Column(name = "new_end_date", nullable = false)
    private LocalDate newEndDate;

    /** Null when the caller set an absolute end date instead of adding months. */
    @Column(name = "months_added")
    private Integer monthsAdded;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;
}
