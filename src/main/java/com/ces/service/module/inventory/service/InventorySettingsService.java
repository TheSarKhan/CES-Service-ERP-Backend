package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventorySettingsRequest;
import com.ces.service.module.inventory.dto.InventorySettingsResponse;
import com.ces.service.module.inventory.entity.InventorySettings;
import com.ces.service.module.inventory.repository.InventorySettingsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-branch warehouse settings, with defaults for branches that never configured any.
 *
 * <p>A branch with no row is not an error state: everything works on defaults until somebody has a
 * reason to change it. That is why reads never fail and writes create the row on demand.
 */
@Service
@Transactional
public class InventorySettingsService {

    private final InventorySettingsRepository repository;
    private final ObjectMapper objectMapper;
    private final String mailHost;

    public InventorySettingsService(
            InventorySettingsRepository repository,
            ObjectMapper objectMapper,
            @Value("${spring.mail.host:}") String mailHost) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.mailHost = mailHost;
    }

    @Transactional(readOnly = true)
    public InventorySettingsResponse get() {
        return toResponse(findOrDefaults(BranchContext.get()));
    }

    public InventorySettingsResponse update(InventorySettingsRequest request) {
        UUID branchId = BranchContext.get();
        InventorySettings settings = repository
                .findByBranchIdAndDeletedAtIsNull(branchId)
                .orElseGet(() -> {
                    InventorySettings created = InventorySettings.builder().build();
                    created.setBranchId(branchId);
                    return created;
                });

        if (request.getNotificationEmails() != null) {
            settings.setNotificationEmails(toJson(normalise(request.getNotificationEmails())));
        }
        if (request.getDailyDigestEnabled() != null) {
            settings.setDailyDigestEnabled(request.getDailyDigestEnabled());
        }
        if (request.getTransferRequiresDifferentReceiver() != null) {
            settings.setTransferRequiresDifferentReceiver(request.getTransferRequiresDifferentReceiver());
        }
        return toResponse(repository.save(settings));
    }

    /** Settings as the rest of the code needs them — never null, defaults when unconfigured. */
    @Transactional(readOnly = true)
    public InventorySettings findOrDefaults(UUID branchId) {
        return repository
                .findByBranchIdAndDeletedAtIsNull(branchId)
                .orElseGet(() -> {
                    InventorySettings defaults = InventorySettings.builder().build();
                    defaults.setBranchId(branchId);
                    return defaults;
                });
    }

    public List<String> recipientsOf(InventorySettings settings) {
        return parseEmails(settings.getNotificationEmails());
    }

    public boolean isMailConfigured() {
        return mailHost != null && !mailHost.isBlank();
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** Trimmed, lower-cased and de-duplicated: the same address twice means two identical mails. */
    private List<String> normalise(List<String> emails) {
        return emails.stream()
                .filter(email -> email != null && !email.isBlank())
                .map(email -> email.trim().toLowerCase())
                .distinct()
                .toList();
    }

    private InventorySettingsResponse toResponse(InventorySettings settings) {
        return InventorySettingsResponse.builder()
                .notificationEmails(parseEmails(settings.getNotificationEmails()))
                .dailyDigestEnabled(settings.getDailyDigestEnabled())
                .transferRequiresDifferentReceiver(settings.getTransferRequiresDifferentReceiver())
                .mailConfigured(isMailConfigured())
                .build();
    }

    private List<String> parseEmails(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // A malformed list must not take the settings screen down with it.
            return List.of();
        }
    }

    private String toJson(List<String> emails) {
        try {
            return objectMapper.writeValueAsString(emails);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
