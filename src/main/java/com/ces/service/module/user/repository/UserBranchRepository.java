package com.ces.service.module.user.repository;

import com.ces.service.module.user.entity.UserBranch;
import com.ces.service.module.user.entity.UserBranchId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@code user_branches}.
 *
 * <p>The entity uses a composite {@link UserBranchId} {@code @EmbeddedId}, so the {@code userId} /
 * {@code branchId} attributes live under {@code id.*}. Derived-name queries cannot resolve them
 * directly, so each method below is expressed with an explicit JPQL {@code @Query} against
 * {@code ub.id.userId} / {@code ub.id.branchId} (method names/signatures are unchanged for callers).
 */
@Repository
public interface UserBranchRepository extends JpaRepository<UserBranch, UserBranchId> {

    @Query("select ub from UserBranch ub where ub.id.userId = :userId")
    List<UserBranch> findByUserId(@Param("userId") UUID userId);

    @Query("select count(ub) > 0 from UserBranch ub "
            + "where ub.id.userId = :userId and ub.id.branchId = :branchId")
    boolean existsByUserIdAndBranchId(
            @Param("userId") UUID userId, @Param("branchId") UUID branchId);

    @Modifying
    @Query("delete from UserBranch ub "
            + "where ub.id.userId = :userId and ub.id.branchId = :branchId")
    void deleteByUserIdAndBranchId(
            @Param("userId") UUID userId, @Param("branchId") UUID branchId);

    @Query("select count(ub) from UserBranch ub where ub.id.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
}
