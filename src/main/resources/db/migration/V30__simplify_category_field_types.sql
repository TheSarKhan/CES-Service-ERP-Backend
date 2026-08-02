-- Shrink inventory_category_fields.field_type to the 6 types actually used by the UI, drop the
-- now-unused options (SELECT/MULTI_SELECT choice list) column, and add show_in_table so a field
-- can be surfaced as its own column in the item table independently of its form visibility.

-- Any existing rows using a type being removed fall back to TEXT rather than being deleted.
UPDATE ces_service.inventory_category_fields
SET field_type = 'TEXT'
WHERE field_type NOT IN ('TEXT', 'NUMBER', 'DATE', 'IMAGE');

ALTER TABLE ces_service.inventory_category_fields
    DROP CONSTRAINT inventory_category_fields_field_type_check;

ALTER TABLE ces_service.inventory_category_fields
    ADD CONSTRAINT inventory_category_fields_field_type_check
    CHECK (field_type IN ('TEXT', 'TEXTAREA', 'NUMBER', 'DATE', 'IMAGE', 'MULTI_IMAGE'));

ALTER TABLE ces_service.inventory_category_fields
    DROP COLUMN options;

ALTER TABLE ces_service.inventory_category_fields
    ADD COLUMN show_in_table BOOLEAN NOT NULL DEFAULT FALSE;
