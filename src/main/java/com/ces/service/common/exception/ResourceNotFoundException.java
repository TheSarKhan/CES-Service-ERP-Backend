package com.ces.service.common.exception;

/**
 * Thrown when a requested entity cannot be located. Maps to HTTP 404 via the
 * {@link ErrorCode#RESOURCE_NOT_FOUND} default (callers may supply a more
 * specific code such as {@link ErrorCode#WO_NOT_FOUND}).
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException() {
        super(ErrorCode.RESOURCE_NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "%s not found: %s".formatted(resource, id));
    }
}
