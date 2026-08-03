package com.ces.service.module.approval.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.common.security.SecurityUtils;
import com.ces.service.module.approval.dto.ApprovalRequestResponse;
import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.entity.ApprovalStatus;
import com.ces.service.module.approval.repository.ApprovalRequestRepository;
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
 * Paralel təsdiqləmə — destructive inventory actions are parked here instead of running.
 *
 * <p>Two rules define the mechanism:
 * <ul>
 *   <li><b>Nobody decides their own request.</b> The requester is recorded at submit time and
 *       compared against the decider — self-approval is rejected outright, so every change is
 *       genuinely seen by a second person.</li>
 *   <li><b>A pending request locks its target.</b> No second request (and no direct mutation)
 *       is accepted for that record until the first one is decided, which keeps the stored
 *       payload from being replayed onto state it was never reviewed against.</li>
 * </ul>
 */
@Service
@Transactional
public class ApprovalService {

    private final ApprovalRequestRepository requestRepository;
    private final Map<ApprovalEntityType, ApprovalExecutor> executors =
            new EnumMap<>(ApprovalEntityType.class);
    /**
     * Spring's configured mapper, not a bare {@code new ObjectMapper()} — response DTOs carry
     * {@code Instant} fields, which a mapper without the JSR-310 module refuses to write. That
     * failure used to be swallowed into an empty snapshot, so reviewers saw no "before" state.
     */
    private final ObjectMapper mapper;

    public ApprovalService(
            ApprovalRequestRepository requestRepository,
            List<ApprovalExecutor> executorList,
            ObjectMapper mapper) {
        this.requestRepository = requestRepository;
        this.mapper = mapper;
        for (ApprovalExecutor executor : executorList) {
            this.executors.put(executor.entityType(), executor);
        }
    }

    /**
     * Parks an operation for review and locks the target.
     *
     * @return the created request, so callers can hand its id back to the client
     */
    public ApprovalRequestResponse submit(
            ApprovalEntityType entityType,
            UUID entityId,
            String entityLabel,
            ApprovalOperation operation,
            Object payload,
            Object beforeSnapshot) {
        assertNotLocked(entityType, entityId);

        ApprovalRequest request = ApprovalRequest.builder()
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

        return ApprovalRequestResponse.from(requestRepository.save(request));
    }

    /** Blocks any direct mutation of a record that already has a change under review. */
    @Transactional(readOnly = true)
    public void assertNotLocked(ApprovalEntityType entityType, UUID entityId) {
        if (findPending(entityType, entityId).isPresent()) {
            throw new BusinessException(ErrorCode.ENTITY_PENDING_APPROVAL);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ApprovalRequest> findPending(ApprovalEntityType entityType, UUID entityId) {
        return requestRepository.findFirstByEntityTypeAndEntityIdAndStatusAndDeletedAtIsNull(
                entityType, entityId, ApprovalStatus.PENDING);
    }

    /** Ids (of the given batch) that currently sit under review — for the "kilidli" list badge. */
    @Transactional(readOnly = true)
    public Set<UUID> findPendingEntityIds(ApprovalEntityType entityType, List<UUID> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(requestRepository.findPendingEntityIds(BranchContext.get(), entityType, entityIds));
    }

    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponse> list(ApprovalStatus status, Pageable pageable) {
        return requestRepository.search(BranchContext.get(), status, pageable)
                .map(ApprovalRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public ApprovalRequestResponse get(UUID id) {
        return ApprovalRequestResponse.from(load(id));
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return requestRepository.countByBranchIdAndStatusAndDeletedAtIsNull(
                BranchContext.get(), ApprovalStatus.PENDING);
    }

    /** Applies the parked operation, then records the decision. */
    public ApprovalRequestResponse approve(UUID id, String note) {
        ApprovalRequest request = loadPendingForDecision(id);

        ApprovalExecutor executor = executors.get(request.getEntityType());
        if (executor == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        // Runs before the status flips: if applying fails, the whole transaction rolls back and
        // the request stays PENDING rather than being marked approved with nothing applied.
        executor.execute(request);

        return ApprovalRequestResponse.from(decide(request, ApprovalStatus.APPROVED, note));
    }

    public ApprovalRequestResponse reject(UUID id, String note) {
        return ApprovalRequestResponse.from(
                decide(loadPendingForDecision(id), ApprovalStatus.REJECTED, note));
    }

    /** The requester withdrawing their own request — releases the lock without applying anything. */
    public ApprovalRequestResponse cancel(UUID id) {
        ApprovalRequest request = load(id);
        assertStillPending(request);
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (request.getRequestedBy() != null && !request.getRequestedBy().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return ApprovalRequestResponse.from(decide(request, ApprovalStatus.CANCELLED, null));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private ApprovalRequest decide(ApprovalRequest request, ApprovalStatus status, String note) {
        request.setStatus(status);
        request.setDecidedBy(SecurityUtils.getCurrentUserId().orElse(null));
        request.setDecidedByName(SecurityUtils.getCurrentEmail().orElse(null));
        request.setDecidedAt(Instant.now());
        request.setDecisionNote(note);
        return requestRepository.save(request);
    }

    private ApprovalRequest loadPendingForDecision(UUID id) {
        ApprovalRequest request = load(id);
        assertStillPending(request);

        // The whole point of the mechanism: a change must be seen by someone other than its author.
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (request.getRequestedBy() != null && request.getRequestedBy().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.APPROVAL_SELF_DECISION);
        }
        return request;
    }

    private void assertStillPending(ApprovalRequest request) {
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessException(ErrorCode.APPROVAL_ALREADY_DECIDED);
        }
    }

    private ApprovalRequest load(UUID id) {
        return requestRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));
    }

    /** Deserializes a stored payload back into the module's request DTO. */
    public <T> T readPayload(ApprovalRequest request, Class<T> type) {
        try {
            return mapper.readValue(request.getPayload(), type);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Fails loudly rather than storing an empty object: a request whose payload didn't serialize
     * would be un-replayable, and one whose snapshot didn't would be reviewed blind.
     */
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
