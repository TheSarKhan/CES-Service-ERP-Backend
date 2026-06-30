-- V20__create_notification_tables.sql
-- Purpose: M12 — notifications (SRS M12.1) + recipient index.

CREATE TABLE ces_service.notifications (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id     UUID          NOT NULL REFERENCES ces_service.branches(id),
    recipient_id  UUID          NOT NULL REFERENCES ces_service.users(id),
    event_type    VARCHAR(100)  NOT NULL,
    title         VARCHAR(255)  NOT NULL,
    body          TEXT          NOT NULL,
    ref_type      VARCHAR(50)   NULL,    -- WORK_ORDER | INSPECTION | VEHICLE ...
    ref_id        UUID          NULL,
    is_read       BOOLEAN       NOT NULL DEFAULT FALSE,
    read_at       TIMESTAMPTZ   NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by    UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by    UUID          NOT NULL REFERENCES ces_service.users(id)
);

CREATE INDEX idx_notifications_recipient
    ON ces_service.notifications(recipient_id, is_read, created_at DESC);
