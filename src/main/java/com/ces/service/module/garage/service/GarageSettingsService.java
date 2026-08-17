package com.ces.service.module.garage.service;

import com.ces.service.common.security.BranchContext;
import com.ces.service.module.garage.dto.GarageSettingsRequest;
import com.ces.service.module.garage.dto.GarageSettingsResponse;
import com.ces.service.module.garage.entity.GarageSettings;
import com.ces.service.module.garage.repository.GarageSettingsRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Branch-scoped Motosaat thresholds (Qaraj Konfiqurasiya's "Tənzimləmələr" tab) — a singleton row
 * per branch, created lazily on first save rather than seeded, so a branch that never opens this
 * screen simply has every threshold read back as "off" ({@link GarageSettingsResponse#empty()}).
 */
@Service
@Transactional
public class GarageSettingsService {

    private final GarageSettingsRepository settingsRepository;
    private final GarageAuditLogger auditLogger;

    public GarageSettingsService(GarageSettingsRepository settingsRepository, GarageAuditLogger auditLogger) {
        this.settingsRepository = settingsRepository;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public GarageSettingsResponse get() {
        UUID branchId = BranchContext.get();
        return settingsRepository.findByBranchIdAndDeletedAtIsNull(branchId)
                .map(GarageSettingsResponse::from)
                .orElseGet(GarageSettingsResponse::empty);
    }

    public GarageSettingsResponse update(GarageSettingsRequest request) {
        UUID branchId = BranchContext.get();
        GarageSettings settings = settingsRepository.findByBranchIdAndDeletedAtIsNull(branchId)
                .orElseGet(() -> {
                    GarageSettings created = new GarageSettings();
                    created.setBranchId(branchId);
                    return created;
                });
        GarageSettingsResponse before = settings.getId() == null
                ? GarageSettingsResponse.empty()
                : GarageSettingsResponse.from(settings);

        settings.setStaleReadingDays(request.getStaleReadingDays());
        settings.setMaxNormalIncreaseEngineHours(request.getMaxNormalIncreaseEngineHours());
        settings.setMaxNormalIncreaseKm(request.getMaxNormalIncreaseKm());
        GarageSettings saved = settingsRepository.save(settings);

        GarageSettingsResponse after = GarageSettingsResponse.from(saved);
        auditLogger.log("UPDATE", "GARAGE_SETTINGS", saved.getId(), before, after);
        return after;
    }
}
