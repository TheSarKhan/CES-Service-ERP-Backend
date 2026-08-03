package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.entity.InventoryCategoryField;
import com.ces.service.module.inventory.enums.InventoryFieldType;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A single dynamic category field definition. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCategoryFieldResponse {

    private UUID id;
    private UUID categoryId;
    private String fieldKey;
    private String label;
    private InventoryFieldType fieldType;
    private Boolean isRequired;
    private String defaultValue;
    private String placeholder;
    private String validationRegex;
    private Integer sortOrder;
    private Boolean isVisible;
    private Boolean showInTable;
    private Boolean isSystem;

    public static InventoryCategoryFieldResponse from(InventoryCategoryField field) {
        return InventoryCategoryFieldResponse.builder()
                .id(field.getId())
                .categoryId(field.getCategoryId())
                .fieldKey(field.getFieldKey())
                .label(field.getLabel())
                .fieldType(field.getFieldType())
                .isRequired(field.getIsRequired())
                .defaultValue(field.getDefaultValue())
                .placeholder(field.getPlaceholder())
                .validationRegex(field.getValidationRegex())
                .sortOrder(field.getSortOrder())
                .isVisible(field.getIsVisible())
                .showInTable(field.getShowInTable())
                .isSystem(field.getIsSystem())
                .build();
    }
}
