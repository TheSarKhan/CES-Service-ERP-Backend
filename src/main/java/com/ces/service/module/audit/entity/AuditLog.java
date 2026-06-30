package com.ces.service.module.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Immutable audit log entry (M17).
 *
 * <p>Does NOT extend {@code BaseEntity}: {@code audit_logs} has no soft delete and no
 * {@code updated_*} columns — the system writes rows and never updates or deletes them.
 * {@code branch_id} and {@code user_id} are nullable (system-level / scheduled-job events).
 * {@code old_value} / {@code new_value} are JSONB, mapped as String. {@code ip_address} is INET,
 * mapped as String (cast handled by the driver / migration).
 */
@Entity
@Table(name = "audit_logs", schema = "ces_service")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** NULL = system-level event. */
    @Column(name = "branch_id")
    private UUID branchId;

    /** NULL = system actor (e.g. scheduled job). */
    @Column(name = "user_id")
    private UUID userId;

    /** Snapshot of the actor's email — retained even if the user is later deleted. */
    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "user_full_name", length = 255)
    private String userFullName;

    /** INET column; stored as text. */
    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "module", nullable = false, length = 100)
    private String module;

    /** CREATE | READ | UPDATE | DELETE | BUSINESS. */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** WORK_ORDER | VEHICLE | USER ... */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    /** Human-readable business identifier (e.g. WO-2026-0042). */
    @Column(name = "entity_number", length = 100)
    private String entityNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    /** SUCCESS | FAILED | DENIED. */
    @Column(name = "result", nullable = false, length = 20)
    private String result;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
