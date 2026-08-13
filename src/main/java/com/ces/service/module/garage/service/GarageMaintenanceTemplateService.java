package com.ces.service.module.garage.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.garage.dto.GarageMaintenanceTemplateItemRequest;
import com.ces.service.module.garage.dto.GarageMaintenanceTemplateItemResponse;
import com.ces.service.module.garage.dto.GarageMaintenanceTemplateRequest;
import com.ces.service.module.garage.dto.GarageMaintenanceTemplateResponse;
import com.ces.service.module.garage.entity.GarageMaintenanceTemplate;
import com.ces.service.module.garage.entity.GarageMaintenanceTemplateItem;
import com.ces.service.module.garage.enums.GarageConfigListType;
import com.ces.service.module.garage.repository.GarageMaintenanceTemplateItemRepository;
import com.ces.service.module.garage.repository.GarageMaintenanceTemplateRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuration-side maintenance templates (e.g. "Ekskavator baxım şablonu"). Applying a template
 * to a specific vehicle and tracking its actual due dates is the Motosaat module's job — this is
 * only the reusable definition, per {@code backend/docs/qaraj-motosaat-plani.md}.
 */
@Service
@Transactional
public class GarageMaintenanceTemplateService {

    private final GarageMaintenanceTemplateRepository templateRepository;
    private final GarageMaintenanceTemplateItemRepository itemRepository;
    private final GarageConfigService configService;
    private final GarageAuditLogger auditLogger;

    public GarageMaintenanceTemplateService(
            GarageMaintenanceTemplateRepository templateRepository,
            GarageMaintenanceTemplateItemRepository itemRepository,
            GarageConfigService configService,
            GarageAuditLogger auditLogger) {
        this.templateRepository = templateRepository;
        this.itemRepository = itemRepository;
        this.configService = configService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public List<GarageMaintenanceTemplateResponse> list() {
        UUID branchId = BranchContext.get();
        return templateRepository.findByBranchIdAndDeletedAtIsNullOrderByEquipmentTypeAscNameAsc(branchId).stream()
                .map(t -> describe(t, itemsOf(t.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GarageMaintenanceTemplateResponse get(UUID id) {
        GarageMaintenanceTemplate template = loadTemplate(id);
        return describe(template, itemsOf(template.getId()));
    }

    public GarageMaintenanceTemplateResponse create(GarageMaintenanceTemplateRequest request) {
        UUID branchId = BranchContext.get();
        assertEachItemHasInterval(request.getItems());
        configService.ensureRegistered(branchId, GarageConfigListType.EQUIPMENT_TYPE, request.getEquipmentType());

        GarageMaintenanceTemplate template = GarageMaintenanceTemplate.builder()
                .equipmentType(request.getEquipmentType())
                .name(request.getName())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        template.setBranchId(branchId);
        GarageMaintenanceTemplate saved = templateRepository.save(template);
        List<GarageMaintenanceTemplateItemResponse> items = saveItems(branchId, saved.getId(), request.getItems());

        GarageMaintenanceTemplateResponse response = describe(saved, items);
        auditLogger.log("CREATE", "GARAGE_MAINTENANCE_TEMPLATE", saved.getId(), null, response);
        return response;
    }

    public GarageMaintenanceTemplateResponse update(UUID id, GarageMaintenanceTemplateRequest request) {
        assertEachItemHasInterval(request.getItems());
        GarageMaintenanceTemplate template = loadTemplate(id);
        GarageMaintenanceTemplateResponse before = describe(template, itemsOf(id));

        template.setEquipmentType(request.getEquipmentType());
        template.setName(request.getName());
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }
        // Templates are small (a handful of lines); replacing them wholesale is simpler and safer
        // than diffing which lines changed, added or were removed.
        itemRepository.deleteByTemplateId(id);
        List<GarageMaintenanceTemplateItemResponse> items = saveItems(template.getBranchId(), id, request.getItems());

        GarageMaintenanceTemplateResponse after = describe(template, items);
        auditLogger.log("UPDATE", "GARAGE_MAINTENANCE_TEMPLATE", id, before, after);
        return after;
    }

    public void delete(UUID id) {
        GarageMaintenanceTemplate template = loadTemplate(id);
        GarageMaintenanceTemplateResponse before = describe(template, itemsOf(id));
        template.setDeletedAt(Instant.now());
        auditLogger.log("DELETE", "GARAGE_MAINTENANCE_TEMPLATE", id, before, null);
    }

    private List<GarageMaintenanceTemplateItemResponse> saveItems(
            UUID branchId, UUID templateId, List<GarageMaintenanceTemplateItemRequest> requests) {
        int order = 0;
        List<GarageMaintenanceTemplateItemResponse> result = new java.util.ArrayList<>();
        for (GarageMaintenanceTemplateItemRequest r : requests) {
            configService.ensureRegistered(branchId, GarageConfigListType.MAINTENANCE_TYPE, r.getMaintenanceType());
            GarageMaintenanceTemplateItem item = GarageMaintenanceTemplateItem.builder()
                    .templateId(templateId)
                    .maintenanceType(r.getMaintenanceType())
                    .intervalMeterHours(r.getIntervalMeterHours())
                    .intervalKm(r.getIntervalKm())
                    .intervalCalendarDays(r.getIntervalCalendarDays())
                    .notes(r.getNotes())
                    .sortOrder(order++)
                    .build();
            item.setBranchId(branchId);
            result.add(GarageMaintenanceTemplateItemResponse.from(itemRepository.save(item)));
        }
        return result;
    }

    /** Same rule the DB CHECK enforces, checked here first so the error names which line is wrong. */
    private void assertEachItemHasInterval(List<GarageMaintenanceTemplateItemRequest> items) {
        for (GarageMaintenanceTemplateItemRequest item : items) {
            if (item.getIntervalMeterHours() == null && item.getIntervalKm() == null
                    && item.getIntervalCalendarDays() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }
    }

    private List<GarageMaintenanceTemplateItemResponse> itemsOf(UUID templateId) {
        return itemRepository.findByTemplateIdAndDeletedAtIsNullOrderBySortOrderAsc(templateId).stream()
                .map(GarageMaintenanceTemplateItemResponse::from)
                .collect(Collectors.toList());
    }

    private GarageMaintenanceTemplateResponse describe(
            GarageMaintenanceTemplate template, List<GarageMaintenanceTemplateItemResponse> items) {
        return GarageMaintenanceTemplateResponse.from(template, items);
    }

    private GarageMaintenanceTemplate loadTemplate(UUID id) {
        return templateRepository.findByIdAndBranchIdAndDeletedAtIsNull(id, BranchContext.get())
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance template not found: " + id));
    }
}
