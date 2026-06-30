package com.ces.service.module.branch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Branch (şöbə) — the multi-branch tenancy anchor (SRS §5.2).
 *
 * <p>Branches do not extend {@code BaseEntity}: the {@code branches} table has no
 * {@code branch_id} self-reference, so the entity declares its own id and audit
 * columns directly.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "branches", schema = "ces_service")
@EntityListeners(AuditingEntityListener.class)
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Nullable to allow seed branches inserted before any user exists.
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private UUID updatedBy;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
