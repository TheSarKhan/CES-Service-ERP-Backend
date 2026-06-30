package com.ces.service.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standard success envelope (SRS §6.2):
 * <pre>{ "success": true, "data": {...}, "meta": {...}, "timestamp": "..." }</pre>
 *
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        PageMeta meta,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, PageMeta meta) {
        return new ApiResponse<>(true, data, meta, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static ApiResponse<Void> empty() {
        return new ApiResponse<>(true, null, null, Instant.now());
    }
}
