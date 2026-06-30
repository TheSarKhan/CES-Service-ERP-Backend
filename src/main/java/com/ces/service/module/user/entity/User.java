package com.ces.service.module.user.entity;

import com.ces.service.common.entity.BaseEntity;
import com.ces.service.module.role.entity.UserRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Application user (M15).
 *
 * <p>Extends {@link BaseEntity} (branch_id + audit columns). Note: per SRS the {@code users}
 * table allows NULL {@code created_by}/{@code updated_by} (first Admin) — the inherited fields
 * tolerate null at the DB level.
 *
 * <p>AuthService depends on these accessors: {@link #getId()}, {@link #getBranchId()},
 * {@link #getFullName()}, {@link #getEmail()}, {@link #getPasswordHash()}, {@link #getIsActive()}
 * / {@link #isActive()}, {@link #getFailedAttempts()}, {@link #getLockedUntil()},
 * {@link #getLastLoginAt()}.
 */
@Entity
@Table(name = "users", schema = "ces_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = {"passwordHash", "userBranches", "userRoles"})
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserBranch> userBranches = new HashSet<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    /** Convenience boolean accessor (AuthService may call {@code isActive()} or {@code getIsActive()}). */
    public boolean isActive() {
        return Boolean.TRUE.equals(this.isActive);
    }
}
