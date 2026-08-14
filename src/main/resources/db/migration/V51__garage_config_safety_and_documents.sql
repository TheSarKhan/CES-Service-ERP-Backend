-- V51__garage_config_safety_and_documents.sql
-- garage_config_values.list_type is DB-CHECK-constrained (V46) as well as Java-enum-constrained
-- (GarageConfigListType) — the vehicle wizard's checklist fields need two new list types there:
-- SAFETY_EQUIPMENT (Təhlükəsizlik avadanlıqları) and MANDATORY_DOCUMENT (Məcburi sənədlər).

ALTER TABLE ces_service.garage_config_values
    DROP CONSTRAINT garage_config_values_list_type_check;
ALTER TABLE ces_service.garage_config_values
    ADD CONSTRAINT garage_config_values_list_type_check CHECK (list_type IN (
        'EQUIPMENT_TYPE', 'BRAND', 'MODEL', 'STATUS', 'LOCATION',
        'DOC_TYPE', 'PHOTO_CATEGORY', 'COMPONENT_TYPE',
        'MAINTENANCE_TYPE', 'METER_SOURCE', 'SAFETY_EQUIPMENT', 'MANDATORY_DOCUMENT'
    ));
