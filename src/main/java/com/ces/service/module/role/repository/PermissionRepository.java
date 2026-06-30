package com.ces.service.module.role.repository;

import com.ces.service.module.role.entity.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Permission catalog repository. The catalog is global (no branch scoping).
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByModule(String module);

    List<Permission> findByModuleOrderByCodeAsc(String module);

    List<Permission> findByCodeIn(Collection<String> codes);

    List<Permission> findByIdIn(Collection<UUID> ids);

    List<Permission> findAllByOrderByModuleAscCodeAsc();

    boolean existsByCode(String code);
}
