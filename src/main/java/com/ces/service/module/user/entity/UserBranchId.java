package com.ces.service.module.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Composite primary key for {@code user_branches}: (user_id, branch_id). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBranchId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserBranchId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(branchId, that.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, branchId);
    }
}
