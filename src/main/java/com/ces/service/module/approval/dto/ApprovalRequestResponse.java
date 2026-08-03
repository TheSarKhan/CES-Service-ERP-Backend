package com.ces.service.module.approval.dto;

import com.ces.service.module.approval.entity.ApprovalEntityType;
import com.ces.service.module.approval.entity.ApprovalOperation;
import com.ces.service.module.approval.entity.ApprovalRequest;
import com.ces.service.module.approval.entity.ApprovalStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;

/**
 * Approval queue row. {@code payload} / {@code beforeSnapshot} are emitted as real JSON (not
 * escaped strings) so the client can diff them field by field without re-parsing.
 */
public record ApprovalRequestResponse(
        UUID id,
        ApprovalEntityType entityType,
        UUID entityId,
        String entityLabel,
        ApprovalOperation operation,
        ApprovalStatus status,
        JsonNode payload,
        JsonNode beforeSnapshot,
        UUID requestedBy,
        String requestedByName,
        Instant requestedAt,
        UUID decidedBy,
        String decidedByName,
        Instant decidedAt,
        String decisionNote) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ApprovalRequestResponse from(ApprovalRequest request) {
        return new ApprovalRequestResponse(
                request.getId(),
                request.getEntityType(),
                request.getEntityId(),
                request.getEntityLabel(),
                request.getOperation(),
                request.getStatus(),
                toNode(request.getPayload()),
                toNode(request.getBeforeSnapshot()),
                request.getRequestedBy(),
                request.getRequestedByName(),
                request.getRequestedAt(),
                request.getDecidedBy(),
                request.getDecidedByName(),
                request.getDecidedAt(),
                request.getDecisionNote());
    }

    private static JsonNode toNode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
