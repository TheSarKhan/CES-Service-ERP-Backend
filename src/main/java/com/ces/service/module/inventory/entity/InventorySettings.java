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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Per-branch warehouse settings.
 *
 * <p>A missing row means defaults, so a branch never has to be set up before it works. Both
 * settings here are policy the user chose rather than rules the code should decide: who the daily
 * digest reaches, and whether a transfer must be received by someone other than its sender —
 * necessary control in a large branch, a total blocker in one with a single storekeeper.
 */
@Entity
@Table(name = "inventory_settings", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InventorySettings extends BaseEntity {

    /** JSON array of addresses, e.g. {@code ["anbar@sirket.az"]}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_emails", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String notificationEmails = "[]";

    @Column(name = "daily_digest_enabled", nullable = false)
    @Builder.Default
    private Boolean dailyDigestEnabled = Boolean.TRUE;

    @Column(name = "transfer_requires_different_receiver", nullable = false)
    @Builder.Default
    private Boolean transferRequiresDifferentReceiver = Boolean.TRUE;
}
