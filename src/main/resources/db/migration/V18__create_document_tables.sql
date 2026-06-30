-- V18__create_document_tables.sql
-- Purpose: M10 — document_templates, generated_documents (SRS M10.1).

CREATE TABLE ces_service.document_templates (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id        UUID          NOT NULL REFERENCES ces_service.branches(id),
    doc_type         VARCHAR(50)   NOT NULL
                     CHECK (doc_type IN ('SERVICE_ACT','INVOICE','OFFER',
                            'HANDOVER_ACT','INTERMEDIARY_REPORT')),
    name             VARCHAR(255)  NOT NULL,
    is_default       BOOLEAN       NOT NULL DEFAULT FALSE,
    template_html    TEXT          NOT NULL,    -- Thymeleaf template (HTML -> PDF)
    header_logo_url  TEXT          NULL,
    company_name     VARCHAR(255)  NULL,
    company_voen     VARCHAR(50)   NULL,
    company_address  TEXT          NULL,
    company_phone    VARCHAR(100)  NULL,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by       UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by       UUID          NOT NULL REFERENCES ces_service.users(id)
);

CREATE TABLE ces_service.generated_documents (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id     UUID          NOT NULL REFERENCES ces_service.branches(id),
    wo_id         UUID          NOT NULL REFERENCES ces_service.work_orders(id),
    template_id   UUID          NOT NULL REFERENCES ces_service.document_templates(id),
    doc_type      VARCHAR(50)   NOT NULL,
    doc_number    VARCHAR(100)  NOT NULL UNIQUE,   -- SRV-ACT-2026-0001
    file_name     VARCHAR(255)  NOT NULL,
    file_url      TEXT          NOT NULL,
    file_size     BIGINT        NULL,
    generated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    generated_by  UUID          NOT NULL REFERENCES ces_service.users(id),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_by    UUID          NOT NULL REFERENCES ces_service.users(id),
    updated_by    UUID          NOT NULL REFERENCES ces_service.users(id)
);
