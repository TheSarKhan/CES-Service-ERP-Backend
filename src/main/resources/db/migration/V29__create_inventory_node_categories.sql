-- V29__create_inventory_node_categories.sql
-- Purpose: optional many-to-many assignment of categories to a Layer node ("bu node-da hansı
-- kateqoriyalar ola bilər"). A node with zero assigned categories is unrestricted (any category
-- may be used when adding an item there) — this table only narrows the choice when populated.

CREATE TABLE ces_service.inventory_node_categories (
    node_id     UUID NOT NULL REFERENCES ces_service.inventory_nodes(id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES ces_service.inventory_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (node_id, category_id)
);

CREATE INDEX idx_inventory_node_categories_category ON ces_service.inventory_node_categories(category_id);
