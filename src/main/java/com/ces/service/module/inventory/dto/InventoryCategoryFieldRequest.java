package com.ces.service.module.inventory.dto;

import com.ces.service.module.inventory.enums.InventoryFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Create / update payload for a single dynamic category field definition. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCategoryFieldRequest {

    @NotBlank
    @Size(max = 100)
    private String fieldKey;

    @NotBlank
    @Size(max = 255)
    private String label;

    @NotNull
    private InventoryFieldType fieldType;

    private Boolean isRequired;

    private String defaultValue;

    @Size(max = 255)
    private String placeholder;

    @Size(max = 500)
    private String validationRegex;

    private Integer sortOrder;

    private Boolean isVisible;

    private Boolean showInTable;
}
