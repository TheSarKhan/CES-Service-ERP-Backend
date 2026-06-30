package com.ces.service.module.role.entity;

import com.ces.service.module.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * User &harr; Role assignment, scoped per branch ({@code user_roles}).
 *
 * <p>Composite PK is (user_id, role_id, branch_id) via {@link UserRoleId}. The relations to
 * {@link User} and {@link Role} are mapped read-only ({@code insertable=false, updatable=false})
 * because the underlying columns are owned by the embedded id. AuthService relies on
 * {@link #getRole()} to walk to {@code Role.getPermissions()} when building the JWT permission union.
 */
@Entity
@Table(name = "user_roles", schema = "ces_service")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"user", "role"})
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private Role role;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    /** Convenience accessor for the branch component of the composite key. */
    public UUID getBranchId() {
        return id == null ? null : id.getBranchId();
    }

    public UUID getUserId() {
        return id == null ? null : id.getUserId();
    }

    public UUID getRoleId() {
        return id == null ? null : id.getRoleId();
    }
}
