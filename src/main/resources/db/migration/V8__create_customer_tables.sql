-- V8__create_customer_tables.sql
-- Purpose: M04 customers (with partial unique indexes) and M05 contractors.
-- contractors is created here (before V9) because service_requests references it.

CREATE TABLE ces_service.customers (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id        UUID          NOT NULL REFERENCES ces_service.branches(id),
    erp_customer_id  UUID          NULL,       -- CES ERP customer UUID
    full_name        VARCHAR(255)  NOT NULL,
    company_name     VARCHAR(255)  NULL,
    voen             VARCHAR(20)   NULL,
    phone            VARCHAR(50)   NULL,
    email            VARCHAR(255)  NULL,
    address          TEXT          NULL,
    customer_type    VARCHAR(50)   NOT NULL DEFAULT 'INDIVIDUAL'
                     CHECK (customer_type IN ('INDIVIDUAL','COMPANY')),
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    notes            TEXT          NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ   NULL,
    created_by       UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by       UUID          NOT NULL REFERENCES ces_service.users(id)
);

CREATE UNIQUE INDEX idx_customers_voen ON ces_service.customers(branch_id, voen)
    WHERE voen IS NOT NULL AND deleted_at IS NULL;
CREATE UNIQUE INDEX idx_customers_erp_id ON ces_service.customers(erp_customer_id)
    WHERE erp_customer_id IS NOT NULL;

CREATE TABLE ces_service.contractors (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id    UUID          NOT NULL REFERENCES ces_service.branches(id),
    name         VARCHAR(255)  NOT NULL,
    phone        VARCHAR(50)   NULL,
    email        VARCHAR(255)  NULL,
    address      TEXT          NULL,
    voen         VARCHAR(20)   NULL,
    is_active    BOOLEAN       NOT NULL DEFAULT TRUE,
    notes        TEXT          NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ   NULL,
    created_by   UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by   UUID          NOT NULL REFERENCES ces_service.users(id)
);
