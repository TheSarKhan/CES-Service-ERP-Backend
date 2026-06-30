package com.ces.service.module.user.entity;

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * User &harr; Branch membership ({@code user_branches}). A user may belong to multiple branches;
 * exactly one should be flagged {@code is_default}.
 */
@Entity
@Table(name = "user_branches", schema = "ces_service")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class UserBranch {

    @EmbeddedId
    private UserBranchId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public UUID getBranchId() {
        return id == null ? null : id.getBranchId();
    }

    public UUID getUserId() {
        return id == null ? null : id.getUserId();
    }
}
