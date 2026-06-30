-- V19__seed_document_templates.sql
-- Purpose: Seed 5 default document templates (one per doc_type) for the default seed branch (HQ),
-- each is_default=true, with a minimal placeholder Thymeleaf HTML stub in template_html.
-- created_by/updated_by use the bootstrap admin user (NOT NULL FK to users).

INSERT INTO ces_service.document_templates
    (branch_id, doc_type, name, is_default, template_html, company_name, created_by, updated_by)
VALUES
('11111111-1111-1111-1111-111111111111','SERVICE_ACT','Standart Servis Aktı',TRUE,
 '<html><head><meta charset="UTF-8"/></head><body><h1>Servis Aktı</h1><p>Sənəd nömrəsi: <span th:text="${docNumber}">SRV-ACT-2026-0000</span></p><p>Work Order: <span th:text="${wo.wo_number}"></span></p><p>Tarix: <span th:text="${generatedAt}"></span></p></body></html>',
 'CES Service','22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','INVOICE','Standart Faktura',TRUE,
 '<html><head><meta charset="UTF-8"/></head><body><h1>Faktura</h1><p>Sənəd nömrəsi: <span th:text="${docNumber}">INV-2026-0000</span></p><p>Müştəri: <span th:text="${customer.full_name}"></span></p><p>Ümumi məbləğ: <span th:text="${costSummary.grand_total_sell}"></span></p></body></html>',
 'CES Service','22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','OFFER','Standart Təklif',TRUE,
 '<html><head><meta charset="UTF-8"/></head><body><h1>Təklif</h1><p>Sənəd nömrəsi: <span th:text="${docNumber}">OFR-2026-0000</span></p><p>Texnika: <span th:text="${vehicle.make}"></span> <span th:text="${vehicle.model}"></span></p></body></html>',
 'CES Service','22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','HANDOVER_ACT','Standart Təhvil Aktı',TRUE,
 '<html><head><meta charset="UTF-8"/></head><body><h1>Təhvil-Təslim Aktı</h1><p>Sənəd nömrəsi: <span th:text="${docNumber}">HND-2026-0000</span></p><p>Work Order: <span th:text="${wo.wo_number}"></span></p></body></html>',
 'CES Service','22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222'),
('11111111-1111-1111-1111-111111111111','INTERMEDIARY_REPORT','Standart Vasitəçilik Hesabatı',TRUE,
 '<html><head><meta charset="UTF-8"/></head><body><h1>Vasitəçilik Hesabatı</h1><p>Sənəd nömrəsi: <span th:text="${docNumber}">INT-2026-0000</span></p><p>Mənfəət: <span th:text="${costSummary.total_profit}"></span></p></body></html>',
 'CES Service','22222222-2222-2222-2222-222222222222','22222222-2222-2222-2222-222222222222');
