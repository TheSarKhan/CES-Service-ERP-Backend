package com.ces.service.module.audit.repository;

import com.ces.service.module.audit.entity.AuditLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Read repository for {@link AuditLog} (M17). Audit logs are append-only — there are no update or
 * delete operations exposed beyond what JpaRepository provides; the service/controller layer (to be
 * built later) will only read. Filters by branch / user / entity / event type with pagination.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByBranchId(UUID branchId, Pageable pageable);

    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findByEventType(String eventType, Pageable pageable);

    Page<AuditLog> findByModule(String module, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    /**
     * Flexible filter — any parameter may be null (ignored). Time window is inclusive of the bounds
     * when provided.
     */
    @Query(
            """
            select a from AuditLog a
            where (:branchId is null or a.branchId = :branchId)
              and (:userId is null or a.userId = :userId)
              and (:eventType is null or a.eventType = :eventType)
              and (:module is null or a.module = :module)
              and (:entityType is null or a.entityType = :entityType)
              and (:entityId is null or a.entityId = :entityId)
              and (:result is null or a.result = :result)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
            """)
    Page<AuditLog> search(
            @Param("branchId") UUID branchId,
            @Param("userId") UUID userId,
            @Param("eventType") String eventType,
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("result") String result,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
