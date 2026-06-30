-- V7__create_vehicle_tables.sql
-- Purpose: M03 Qaraj — vehicles, vehicle_documents (SRS M03.1) + indexes.

CREATE TABLE ces_service.vehicles (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id        UUID          NOT NULL REFERENCES ces_service.branches(id),
    garage_type      VARCHAR(50)   NOT NULL
                     CHECK (garage_type IN ('COMPANY','INVESTOR','CUSTOMER')),
    owner_id         UUID          NULL,   -- investors.id | customers.id (per garage_type)
    make             VARCHAR(100)  NOT NULL,
    model            VARCHAR(100)  NOT NULL,
    year             SMALLINT      NULL,
    chassis_number   VARCHAR(100)  NULL UNIQUE,
    serial_number    VARCHAR(100)  NULL,
    plate_number     VARCHAR(50)   NULL,
    vehicle_type     VARCHAR(100)  NULL,
    status           VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE','IN_SERVICE','INACTIVE')),
    current_location TEXT          NULL,
    notes            TEXT          NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ   NULL,
    created_by       UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by       UUID          NOT NULL REFERENCES ces_service.users(id)
);

CREATE INDEX idx_vehicles_branch_status ON ces_service.vehicles(branch_id, status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_vehicles_garage_type ON ces_service.vehicles(branch_id, garage_type)
    WHERE deleted_at IS NULL;

CREATE TABLE ces_service.vehicle_documents (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    vehicle_id   UUID          NOT NULL REFERENCES ces_service.vehicles(id) ON DELETE CASCADE,
    doc_type     VARCHAR(100)  NOT NULL,
    file_name    VARCHAR(255)  NOT NULL,
    file_url     TEXT          NOT NULL,
    file_size    BIGINT        NULL,
    expires_at   DATE          NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    deleted_at   TIMESTAMPTZ   NULL
);
