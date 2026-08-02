package com.ces.service.module.inventory.service;

import com.ces.service.common.security.BranchContext;
import com.ces.service.common.security.SecurityUtils;
import com.ces.service.module.audit.entity.AuditLog;
import com.ces.service.module.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Writes inventory stock actions to the shared, generic {@link AuditLog} table (module =
 * {@code WAREHOUSE}) — SRS "Audit Log" section 5. Reused as-is; no dedicated inventory audit
 * table exists or is needed.
 */
@Component
public class InventoryAuditLogger {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AuditLogRepository auditLogRepository;

    public InventoryAuditLogger(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** @param action CREATE | UPDATE | DELETE | BUSINESS */
    public void log(String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        AuditLog entry = AuditLog.builder()
                .branchId(BranchContext.get())
                .userId(SecurityUtils.getCurrentUserId().orElse(null))
                .userEmail(SecurityUtils.getCurrentEmail().orElse(null))
                .eventType("INVENTORY_" + action)
                .module("WAREHOUSE")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .result("SUCCESS")
                .build();
        auditLogRepository.save(entry);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }
}
