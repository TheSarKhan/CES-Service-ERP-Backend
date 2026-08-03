package com.ces.service.module.approval.entity;

import com.ces.service.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
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
 * A destructive action waiting on a second pair of eyes.
 *
 * <p>The action is not applied when requested: {@code payload} holds the request body, which is
 * replayed against the owning module's service once someone else approves. While a request is
 * {@code PENDING} its target entity is locked — a partial unique index guarantees at most one
 * pending request per entity, so two people can't queue conflicting changes.
 *
 * <p>{@code entityLabel} and {@code requestedByName} are snapshots taken at request time so the
 * queue stays readable even after the target is deleted or a user is renamed.
 */
@Entity
@Table(name = "approval_requests", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ApprovalRequest extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private ApprovalEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "entity_label", length = 255)
    private String entityLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 50)
    private ApprovalOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private String payload = "{}";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_snapshot", columnDefinition = "jsonb")
    private String beforeSnapshot;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "requested_by_name", length = 255)
    private String requestedByName;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private Instant requestedAt = Instant.now();

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_by_name", length = 255)
    private String decidedByName;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", columnDefinition = "text")
    private String decisionNote;
}
