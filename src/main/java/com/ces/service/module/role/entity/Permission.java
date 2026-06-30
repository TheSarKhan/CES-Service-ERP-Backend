package com.ces.service.module.role.entity;

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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Permission catalog entry (M16).
 *
 * <p>The {@code permissions} table is GLOBAL: it has NO {@code branch_id} and NO soft delete
 * ({@code deleted_at}). It therefore does NOT extend {@code BaseEntity}; it defines its own id and
 * audit columns. The catalog is only mutated via Flyway migrations (RBAC-V06).
 */
@Entity
@Table(name = "permissions", schema = "ces_service")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "module", nullable = false, length = 100)
    private String module;

    /** One of CRUD | BUSINESS | REPORT | SYSTEM. */
    @Column(name = "perm_type", nullable = false, length = 50)
    private String permType;

    /** GET|POST|PUT|DELETE for CRUD permissions; nullable for others. */
    @Column(name = "http_method", length = 20)
    private String httpMethod;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;
}
