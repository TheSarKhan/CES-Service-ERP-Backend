-- Every category now auto-seeds 4 non-deletable "system" dynamic fields at creation time
-- (Şəkil, Açıqlama, İstehsalçı/Təchizatçı, Vəziyyət — see InventoryCategoryService). This column
-- marks those rows so the delete endpoint can reject removing them.
ALTER TABLE ces_service.inventory_category_fields
    ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;
