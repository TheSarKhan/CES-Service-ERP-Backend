package com.ces.service.module.role.repository;

import com.ces.service.module.role.entity.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Branch-scoped repository for {@link Role}. All finders exclude soft-deleted rows
 * ({@code deleted_at IS NULL}).
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    Optional<Role> findByBranchIdAndCodeAndDeletedAtIsNull(UUID branchId, String code);

    List<Role> findByBranchIdAndDeletedAtIsNull(UUID branchId);

    Page<Role> findByBranchIdAndDeletedAtIsNull(UUID branchId, Pageable pageable);

    boolean existsByBranchIdAndCodeAndDeletedAtIsNull(UUID branchId, String code);
}
