package com.ces.service.module.branch.repository;

import com.ces.service.module.branch.entity.Branch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    Optional<Branch> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Branch> findByCodeAndDeletedAtIsNull(String code);

    boolean existsByCodeAndDeletedAtIsNull(String code);

    Page<Branch> findAllByDeletedAtIsNull(Pageable pageable);
}
