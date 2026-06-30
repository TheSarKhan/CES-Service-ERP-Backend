package com.ces.service.common.exception;

/**
 * Base runtime exception carrying an {@link ErrorCode} (and optional message
 * argument overrides). The {@link GlobalExceptionHandler} converts it into the
 * standard error envelope with the code's mapped HTTP status.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object[] args;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    public BusinessException(ErrorCode errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args != null ? args : new Object[0];
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
