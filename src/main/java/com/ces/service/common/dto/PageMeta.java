package com.ces.service.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pagination metadata included in list responses (SRS §6.2).
 *
 * @param page       current page number (1-based)
 * @param size       page size
 * @param totalPages total number of pages
 * @param totalItems total number of items across all pages
 */
public record PageMeta(
        int page,
        int size,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("total_items") long totalItems
) {
}
