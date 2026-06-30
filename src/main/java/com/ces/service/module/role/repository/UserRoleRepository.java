package com.ces.service.module.role.repository;

import com.ces.service.module.role.entity.UserRole;
import com.ces.service.module.role.entity.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@code user_roles}.
 *
 * <p>{@link #findByUserIdAndBranchId(UUID, UUID)} returns the user's role assignments for a branch;
 * AuthService walks {@code UserRole.getRole().getPermissions()} over this list to build the JWT
 * permission union. {@link #findPermissionCodesByUserIdAndBranchId(UUID, UUID)} is a direct,
 * cheaper query that returns the distinct permission code set for a user within a branch.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("select ur from UserRole ur "
            + "where ur.id.userId = :userId and ur.id.branchId = :branchId")
    List<UserRole> findByUserIdAndBranchId(
            @Param("userId") UUID userId, @Param("branchId") UUID branchId);

    @Query(
            """
            select ur from UserRole ur
            join fetch ur.role r
            left join fetch r.permissions
            where ur.id.userId = :userId and ur.id.branchId = :branchId
            """)
    List<UserRole> findByUserIdAndBranchIdWithPermissions(
            @Param("userId") UUID userId, @Param("branchId") UUID branchId);

    @Query("select ur from UserRole ur where ur.id.userId = :userId")
    List<UserRole> findByUserId(@Param("userId") UUID userId);

    @Query("select ur from UserRole ur where ur.id.roleId = :roleId")
    List<UserRole> findByRoleId(@Param("roleId") UUID roleId);

    @Query("select count(ur) > 0 from UserRole ur where ur.id.roleId = :roleId")
    boolean existsByRoleId(@Param("roleId") UUID roleId);

    @Query("select count(ur) > 0 from UserRole ur "
            + "where ur.id.userId = :userId and ur.id.roleId = :roleId "
            + "and ur.id.branchId = :branchId")
    boolean existsByUserIdAndRoleIdAndBranchId(
            @Param("userId") UUID userId,
            @Param("roleId") UUID roleId,
            @Param("branchId") UUID branchId);

    @Query("select count(ur) from UserRole ur where ur.id.roleId = :roleId")
    long countByRoleId(@Param("roleId") UUID roleId);

    @Modifying
    @Query("delete from UserRole ur "
            + "where ur.id.userId = :userId and ur.id.roleId = :roleId "
            + "and ur.id.branchId = :branchId")
    void deleteByUserIdAndRoleIdAndBranchId(
            @Param("userId") UUID userId,
            @Param("roleId") UUID roleId,
            @Param("branchId") UUID branchId);

    /**
     * Distinct permission codes for a user within a branch — used by AuthService to populate the
     * JWT {@code permissions} claim.
     */
    @Query(
            """
            select distinct p.code from UserRole ur
            join ur.role r
            join r.permissions p
            where ur.id.userId = :userId
              and ur.id.branchId = :branchId
              and r.deletedAt is null
              and r.isActive = true
              and p.isActive = true
            """)
    List<String> findPermissionCodesByUserIdAndBranchId(
            @Param("userId") UUID userId, @Param("branchId") UUID branchId);

    /** Distinct role codes for a user within a branch — used for the JWT {@code roles} claim. */
    @Query(
            """
            select distinct r.code from UserRole ur
            join ur.role r
            where ur.id.userId = :userId
              and ur.id.branchId = :branchId
              and r.deletedAt is null
            """)
    List<String> findRoleCodesByUserIdAndBranchId(
            @Param("userId") UUID userId, @Param("branchId") UUID branchId);

    /**
     * Count of active (non-deleted) users that currently hold the given role code across the branch.
     * Used by the "last admin" guard (USR-V08).
     */
    @Query(
            """
            select count(distinct ur.id.userId) from UserRole ur
            join ur.role r
            join ur.user u
            where r.code = :roleCode
              and ur.id.branchId = :branchId
              and r.deletedAt is null
              and u.deletedAt is null
              and u.isActive = true
            """)
    long countActiveUsersByRoleCodeAndBranchId(
            @Param("roleCode") String roleCode, @Param("branchId") UUID branchId);
}
