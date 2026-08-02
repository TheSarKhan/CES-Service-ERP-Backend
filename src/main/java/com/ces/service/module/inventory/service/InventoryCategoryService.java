package com.ces.service.module.inventory.service;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import com.ces.service.common.exception.ResourceNotFoundException;
import com.ces.service.common.security.BranchContext;
import com.ces.service.module.inventory.dto.InventoryCategoryFieldRequest;
import com.ces.service.module.inventory.dto.InventoryCategoryFieldResponse;
import com.ces.service.module.inventory.dto.InventoryCategoryRequest;
import com.ces.service.module.inventory.dto.InventoryCategoryResponse;
import com.ces.service.module.inventory.entity.InventoryCategory;
import com.ces.service.module.inventory.entity.InventoryCategoryField;
import com.ces.service.module.inventory.repository.InventoryCategoryFieldRepository;
import com.ces.service.module.inventory.repository.InventoryCategoryRepository;
import com.ces.service.module.inventory.repository.InventoryItemRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Category configuration (Kateqoriya Konfiqurasiyası) — categories own a dynamic field schema
 * (EAV) that item forms render themselves from.
 *
 * <p>Business rules:
 * <ul>
 *   <li>Category name unique within branch.</li>
 *   <li>Field {@code fieldKey} unique within its category.</li>
 *   <li>A category with items cannot be deleted → {@link ErrorCode#CATEGORY_NOT_EMPTY}
 *       (wired once the Item module exists).</li>
 * </ul>
 */
@Service
@Transactional
public class InventoryCategoryService {

    private final InventoryCategoryRepository categoryRepository;
    private final InventoryCategoryFieldRepository fieldRepository;
    private final InventoryItemRepository itemRepository;

    public InventoryCategoryService(
            InventoryCategoryRepository categoryRepository,
            InventoryCategoryFieldRepository fieldRepository,
            InventoryItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.fieldRepository = fieldRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<InventoryCategoryResponse> list() {
        UUID branchId = BranchContext.get();
        List<InventoryCategory> categories = categoryRepository.findByBranchIdAndDeletedAtIsNullOrderByNameAsc(branchId);
        if (categories.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = categories.stream().map(InventoryCategory::getId).collect(Collectors.toList());
        Map<UUID, List<InventoryCategoryFieldResponse>> fieldsByCategory =
                fieldRepository.findByCategoryIdInOrderBySortOrderAsc(ids).stream()
                        .map(InventoryCategoryFieldResponse::from)
                        .collect(Collectors.groupingBy(InventoryCategoryFieldResponse::getCategoryId));
        return categories.stream()
                .map(c -> InventoryCategoryResponse.from(c, fieldsByCategory.getOrDefault(c.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryCategoryResponse get(UUID id) {
        InventoryCategory category = loadCategory(id);
        return InventoryCategoryResponse.from(category, listFields(category.getId()));
    }

    public InventoryCategoryResponse create(InventoryCategoryRequest request) {
        UUID branchId = BranchContext.get();

        if (categoryRepository.existsByBranchIdAndNameAndDeletedAtIsNull(branchId, request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        InventoryCategory category = InventoryCategory.builder()
                .name(request.getName())
                .defaultUnit(request.getDefaultUnit())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .build();
        category.setBranchId(branchId);
        InventoryCategory saved = categoryRepository.save(category);

        List<InventoryCategoryFieldResponse> fields = new ArrayList<>();
        if (request.getFields() != null) {
            for (InventoryCategoryFieldRequest fieldRequest : request.getFields()) {
                fields.add(saveField(saved.getId(), fieldRequest));
            }
        }
        return InventoryCategoryResponse.from(saved, fields);
    }

    public InventoryCategoryResponse update(UUID id, InventoryCategoryRequest request) {
        InventoryCategory category = loadCategory(id);

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByBranchIdAndNameAndDeletedAtIsNullAndIdNot(
                        category.getBranchId(), request.getName(), category.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }

        category.setName(request.getName());
        category.setDefaultUnit(request.getDefaultUnit());
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }
        return InventoryCategoryResponse.from(category, listFields(category.getId()));
    }

    public void delete(UUID id) {
        InventoryCategory category = loadCategory(id);
        if (itemRepository.existsByCategoryIdAndDeletedAtIsNull(category.getId())) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_EMPTY);
        }
        category.setDeletedAt(Instant.now());
        categoryRepository.save(category);
    }

    // ── dynamic fields ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InventoryCategoryFieldResponse> listFields(UUID categoryId) {
        loadCategory(categoryId);
        return fieldRepository.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(InventoryCategoryFieldResponse::from)
                .collect(Collectors.toList());
    }

    public InventoryCategoryFieldResponse addField(UUID categoryId, InventoryCategoryFieldRequest request) {
        loadCategory(categoryId);
        return saveField(categoryId, request);
    }

    public InventoryCategoryFieldResponse updateField(UUID categoryId, UUID fieldId, InventoryCategoryFieldRequest request) {
        loadCategory(categoryId);
        InventoryCategoryField field = fieldRepository
                .findByIdAndCategoryId(fieldId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category field not found: " + fieldId));

        if (!field.getFieldKey().equals(request.getFieldKey())
                && fieldRepository.existsByCategoryIdAndFieldKeyAndIdNot(categoryId, request.getFieldKey(), fieldId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_FIELD_KEY);
        }

        applyFieldRequest(field, request);
        return InventoryCategoryFieldResponse.from(field);
    }

    public void removeField(UUID categoryId, UUID fieldId) {
        loadCategory(categoryId);
        InventoryCategoryField field = fieldRepository
                .findByIdAndCategoryId(fieldId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category field not found: " + fieldId));
        fieldRepository.delete(field);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private InventoryCategoryFieldResponse saveField(UUID categoryId, InventoryCategoryFieldRequest request) {
        if (fieldRepository.existsByCategoryIdAndFieldKey(categoryId, request.getFieldKey())) {
            throw new BusinessException(ErrorCode.DUPLICATE_FIELD_KEY);
        }
        InventoryCategoryField field = new InventoryCategoryField();
        field.setCategoryId(categoryId);
        applyFieldRequest(field, request);
        InventoryCategoryField saved = fieldRepository.save(field);
        return InventoryCategoryFieldResponse.from(saved);
    }

    private void applyFieldRequest(InventoryCategoryField field, InventoryCategoryFieldRequest request) {
        field.setFieldKey(request.getFieldKey());
        field.setLabel(request.getLabel());
        field.setFieldType(request.getFieldType());
        field.setIsRequired(request.getIsRequired() == null ? Boolean.FALSE : request.getIsRequired());
        field.setDefaultValue(request.getDefaultValue());
        field.setPlaceholder(request.getPlaceholder());
        field.setValidationRegex(request.getValidationRegex());
        field.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        field.setIsVisible(request.getIsVisible() == null ? Boolean.TRUE : request.getIsVisible());
        field.setShowInTable(request.getShowInTable() == null ? Boolean.FALSE : request.getShowInTable());
    }

    /** Reorders a category's fields to match the given id sequence (index becomes sort_order). */
    public List<InventoryCategoryFieldResponse> reorderFields(UUID categoryId, List<UUID> fieldIds) {
        loadCategory(categoryId);
        List<InventoryCategoryField> fields = fieldRepository.findByCategoryIdOrderBySortOrderAsc(categoryId);

        if (fieldIds.size() != fields.size() || !fieldIds.containsAll(
                fields.stream().map(InventoryCategoryField::getId).collect(Collectors.toList()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        Map<UUID, InventoryCategoryField> byId =
                fields.stream().collect(Collectors.toMap(InventoryCategoryField::getId, f -> f));
        for (int i = 0; i < fieldIds.size(); i++) {
            byId.get(fieldIds.get(i)).setSortOrder(i);
        }
        fieldRepository.saveAll(fields);
        return fieldRepository.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(InventoryCategoryFieldResponse::from)
                .collect(Collectors.toList());
    }

    private InventoryCategory loadCategory(UUID id) {
        UUID branchId = BranchContext.get();
        return categoryRepository
                .findByIdAndBranchIdAndDeletedAtIsNull(id, branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory category not found: " + id));
    }
}
