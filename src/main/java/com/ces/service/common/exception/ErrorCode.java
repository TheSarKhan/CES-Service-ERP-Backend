package com.ces.service.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Canonical catalogue of error codes returned by the API (SRS §6.5 plus the
 * business-rule violations scattered across the module validation sections).
 *
 * <p>Each code pairs a stable string identifier (the value placed in the error
 * envelope) with an {@link HttpStatus} and a sensible default message.</p>
 */
public enum ErrorCode {

    // ── Authentication / Authorization ──────────────────────────────────────
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token has expired"),
    AUTH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "Token has been revoked"),
    AUTH_ACCOUNT_LOCKED(HttpStatus.UNAUTHORIZED, "Account is locked due to too many failed attempts"),
    AUTH_ACCOUNT_INACTIVE(HttpStatus.UNAUTHORIZED, "Account is inactive"),
    BRANCH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "X-Branch-Id is not among the branches of this token"),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "Required permission is missing"),

    // ── Generic resource ────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    WO_NOT_FOUND(HttpStatus.NOT_FOUND, "Work order not found"),
    VEHICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Vehicle not found"),

    // ── Conflicts (409) ─────────────────────────────────────────────────────
    DUPLICATE_VOEN(HttpStatus.CONFLICT, "This VOEN already exists"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "This email already exists"),
    DUPLICATE_ROLE_CODE(HttpStatus.CONFLICT, "This role code already exists"),
    DUPLICATE_CHASSIS_NUMBER(HttpStatus.CONFLICT, "This chassis number already exists"),

    // ── Business-rule violations (422) ──────────────────────────────────────
    WEAK_PASSWORD(HttpStatus.UNPROCESSABLE_ENTITY, "Password does not meet the security policy"),
    WO_INVALID_STATUS_TRANSITION(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid work order status transition"),
    STOCK_INSUFFICIENT(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient stock"),
    ENGINE_HOURS_DECREASING(HttpStatus.UNPROCESSABLE_ENTITY, "Engine hours cannot decrease"),
    COST_BELOW_UNIT_COST(HttpStatus.UNPROCESSABLE_ENTITY, "Sell price is below unit cost"),
    WO_CLOSE_REQUIRES_COST(HttpStatus.UNPROCESSABLE_ENTITY, "A work order requires cost items before it can be closed"),
    VEHICLE_IN_SERVICE(HttpStatus.UNPROCESSABLE_ENTITY, "Vehicle is currently in service"),
    VEHICLE_HAS_ACTIVE_WO(HttpStatus.UNPROCESSABLE_ENTITY, "Vehicle has an active work order"),
    OWNER_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "owner_id is required for INVESTOR / CUSTOMER vehicles"),
    ROLE_BRANCH_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "Role branch must be one of the user's branches"),
    SR_NO_SERVICES(HttpStatus.UNPROCESSABLE_ENTITY, "Service request must contain at least one service"),
    SR_INVALID_HOURS(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid service hours"),
    SR_INVALID_VEHICLE_TYPE(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid vehicle type for this service request"),
    SR_CONTRACTOR_AMOUNT_EXCEEDS(HttpStatus.UNPROCESSABLE_ENTITY, "Contractor amount exceeds the allowed limit"),
    CATEGORY_NOT_EMPTY(HttpStatus.UNPROCESSABLE_ENTITY, "Category is not empty and cannot be removed"),
    SYSTEM_ROLE_PROTECTED(HttpStatus.UNPROCESSABLE_ENTITY, "System roles are protected and cannot be modified or deleted"),
    ROLE_HAS_ACTIVE_USERS(HttpStatus.UNPROCESSABLE_ENTITY, "Role still has active users assigned"),
    LAST_ADMIN(HttpStatus.UNPROCESSABLE_ENTITY, "The last administrator cannot be removed or demoted"),
    CANNOT_DEACTIVATE_SELF(HttpStatus.UNPROCESSABLE_ENTITY, "A user cannot deactivate their own account"),
    USER_HAS_ACTIVE_WO(HttpStatus.UNPROCESSABLE_ENTITY, "User has active work orders assigned"),
    ARCHIVE_IMMUTABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Archived records are immutable"),

    // ── Rate limiting (429) ─────────────────────────────────────────────────
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests — please wait"),

    // ── Validation / generic (400 / 500) ────────────────────────────────────
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
