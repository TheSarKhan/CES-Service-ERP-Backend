package com.ces.service.module.garageapproval.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.common.security.SecurityUtils;
import com.ces.service.module.approval.entity.ApprovalStatus;
import com.ces.service.module.garageapproval.dto.GarageApprovalRequestResponse;
import com.ces.service.module.garageapproval.entity.GarageApprovalEntityType;
import com.ces.service.module.garageapproval.entity.GarageApprovalOperation;
import com.ces.service.module.garageapproval.entity.GarageApprovalRequest;
import com.ces.service.module.garageapproval.repository.GarageApprovalRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qaraj/Motosaat's own approval queue — a deliberately separate twin of Anbarın
 * {@code ApprovalService}. See migration V49 for why: the user wants the two queues to never
 * mix, each with its own table, service, and (on the frontend) its own page under the Qaraj
 * sidebar entry rather than sharing Anbarın Təsdiqləmələr screen.
 *
 * <p>Same two rules as Anbarın version:
 * <ul>
 *   <li><b>Nobody decides their own request.</b></li>
 *   <li><b>A pending request locks its target</b> until it's decided.</li>
 * </ul>
 */
@Service
@Transactional
public class GarageApprovalService {

    private final GarageApprovalRequestRepository requestRepository;
    private final Map<GarageApprovalEntityType, GarageApprovalExecutor> executors =
            new EnumMap<>(GarageApprovalEntityType.class);
    private final ObjectMapper mapper;

    public GarageApprovalService(
            GarageApprovalRequestRepository requestRepository,
            List<GarageApprovalExecutor> executorList,
            ObjectMapper mapper) {
        this.requestRepository = requestRepository;
        this.mapper = mapper;
        for (GarageApprovalExecutor executor : executorList) {
            this.executors.put(executor.entityType(), executor);
        }
    }

    public GarageApprovalRequestResponse submit(
            GarageApprovalEntityType entityType,
            UUID entityId,
            String entityLabel,
            GarageApprovalOperation operation,
            Object payload,
            Object beforeSnapshot) {
        assertNotLocked(entityType, entityId);

        GarageApprovalRequest request = GarageApprovalRequest.builder()
                .entityType(entityType)
                .entityId(entityId)
                .entityLabel(entityLabel)
                .operation(operation)
                .status(ApprovalStatus.PENDING)
                .payload(toJson(payload))
                .beforeSnapshot(beforeSnapshot == null ? null : toJson(beforeSnapshot))
                .requestedBy(SecurityUtils.getCurrentUserId().orElse(null))
                .requestedByName(SecurityUtils.getCurrentEmail().orElse(null))
                .requestedAt(Instant.now())
                .build();
        request.setBranchId(BranchContext.get());

        return GarageApprovalRequestResponse.from(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public void assertNotLocked(GarageApprovalEntityType entityType, UUID entityId) {
        if (findPending(entityType, entityId).isPresent()) {
            throw new BusinessException(ErrorCode.ENTITY_PENDING_APPROVAL);
        }
    }

    @Transactional(readOnly = true)
    public Optional<GarageApprovalRequest> findPending(GarageApprovalEntityType entityType, UUID entityId) {
        return requestRepository.findFirstByEntityTypeAndEntityIdAndStatusAndDeletedAtIsNull(
                entityType, entityId, ApprovalStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Set<UUID> findPendingEntityIds(GarageApprovalEntityType entityType, List<UUID> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(requestRepository.findPendingEntityIds(BranchContext.get(), entityType, entityIds));
    }

    @Transactional(readOnly = true)
    public Page<GarageApprovalRequestResponse> list(ApprovalStatus status, Pageable pageable) {
        return requestRepository.search(BranchContext.get(), status, pageable)
                .map(GarageApprovalRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public GarageApprovalRequestResponse get(UUID id) {
        return GarageApprovalRequestResponse.from(load(id));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return requestRepository.countByBranchIdAndStatusAndDeletedAtIsNull(
                BranchContext.get(), ApprovalStatus.PENDING);
    }

    public GarageApprovalRequestResponse approve(UUID id, String note) {
        GarageApprovalRequest request = loadPendingForDecision(id);
        executorFor(request).execute(request);
        return GarageApprovalRequestResponse.from(decide(request, ApprovalStatus.APPROVED, note));
    }

    public GarageApprovalRequestResponse reject(UUID id, String note) {
        GarageApprovalRequest request = loadPendingForDecision(id);
        return GarageApprovalRequestResponse.from(decide(request, ApprovalStatus.REJECTED, note));
    }

    private GarageApprovalExecutor executorFor(GarageApprovalRequest request) {
        GarageApprovalExecutor executor = executors.get(request.getEntityType());
        if (executor == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return executor;
    }

    public GarageApprovalRequestResponse cancel(UUID id) {
        GarageApprovalRequest request = load(id);
        assertStillPending(request);
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (request.getRequestedBy() != null && !request.getRequestedBy().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return GarageApprovalRequestResponse.from(decide(request, ApprovalStatus.CANCELLED, null));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private GarageApprovalRequest decide(GarageApprovalRequest request, ApprovalStatus status, String note) {
        request.setStatus(status);
        request.setDecidedBy(SecurityUtils.getCurrentUserId().orElse(null));
        request.setDecidedByName(SecurityUtils.getCurrentEmail().orElse(null));
        request.setDecidedAt(Instant.now());
        request.setDecisionNote(note);
        return requestRepository.save(request);
    }

    private GarageApprovalRequest loadPendingForDecision(UUID id) {
        GarageApprovalRequest request = load(id);
        assertStillPending(request);
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (request.getRequestedBy() != null && request.getRequestedBy().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.APPROVAL_SELF_DECISION);
        }
        return request;
    }

    private void assertStillPending(GarageApprovalRequest request) {
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DECIDED);
        }
    }

    private GarageApprovalRequest load(UUID id) {
        return requestRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Garage approval request not found: " + id));
    }

    public <T> T readPayload(GarageApprovalRequest request, Class<T> type) {
        try {
            return mapper.readValue(request.getPayload(), type);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }
}
