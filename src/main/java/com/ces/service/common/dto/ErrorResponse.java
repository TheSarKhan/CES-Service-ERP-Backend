package com.ces.service.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope (SRS §6.2):
 * <pre>
 * { "success": false,
 *   "error": { "code": "WO_NOT_FOUND", "message": "...", "details": [...] },
 *   "timestamp": "..." }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        ErrorBody error,
        Instant timestamp
) {

    public static ErrorResponse of(String code, String message, List<FieldError> details) {
        return new ErrorResponse(false, new ErrorBody(code, message, details), Instant.now());
    }

    public static ErrorResponse of(String code, String message) {
        return of(code, message, null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
            String code,
            String message,
            List<FieldError> details
    ) {
    }

    /**
     * A single field-level validation failure.
     *
     * @param field   the offending field/property name
     * @param message the human-readable validation message
     */
    public record FieldError(
            String field,
            String message
    ) {
    }
}
