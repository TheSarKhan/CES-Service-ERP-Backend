package com.ces.service.common.exception;

import com.ces.service.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;

import java.util.List;

/**
 * Centralised translation of exceptions into the standard {@link ErrorResponse}
 * envelope (SRS §6.2 / §6.5).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        log.debug("Business exception [{}]: {}", code, ex.getMessage());
        return build(code, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorResponse.FieldError> details = ex.getConstraintViolations().stream()
                .map(v -> new ErrorResponse.FieldError(
                        v.getPropertyPath() != null ? v.getPropertyPath().toString() : null,
                        v.getMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse.FieldError detail = new ErrorResponse.FieldError(
                ex.getName(),
                "Invalid value: " + ex.getValue());
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.getDefaultMessage(), List.of(detail));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        return build(ErrorCode.PERMISSION_DENIED, ErrorCode.PERMISSION_DENIED.getDefaultMessage(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.debug("Authentication failure: {}", ex.getMessage());
        return build(ErrorCode.AUTH_INVALID_CREDENTIALS, ErrorCode.AUTH_INVALID_CREDENTIALS.getDefaultMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), null);
    }

    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> build(ErrorCode code, String message, List<ErrorResponse.FieldError> details) {
        HttpStatus status = code.getStatus();
        ErrorResponse body = ErrorResponse.of(code.name(), message, details);
        return ResponseEntity.status(status).body(body);
    }
}
