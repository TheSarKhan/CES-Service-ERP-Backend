package com.ces.service.module.user.repository;

import com.ces.service.module.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link User}. Email is globally unique (USR-V01) so email lookups are not
 * branch-scoped; list/detail finders are branch-scoped and exclude soft-deleted rows.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    Optional<User> findByIdAndBranchIdAndDeletedAtIsNull(UUID id, UUID branchId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    List<User> findByBranchIdAndDeletedAtIsNull(UUID branchId);

    Page<User> findByBranchIdAndDeletedAtIsNull(UUID branchId, Pageable pageable);

    Page<User> findByBranchIdAndIsActiveAndDeletedAtIsNull(
            UUID branchId, Boolean isActive, Pageable pageable);
}
