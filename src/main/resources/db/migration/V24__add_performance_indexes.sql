-- V24__add_performance_indexes.sql
-- Purpose: Additional composite indexes for multi-branch performance (SRS §5.7) that were not
-- already created in earlier migrations. Uses CREATE INDEX IF NOT EXISTS to stay idempotent and
-- to avoid clashing with indexes created earlier:
--   - idx_vehicles_branch_status      already created in V7
--   - idx_vehicles_garage_type        already created in V7
--   - idx_spare_parts_branch_cat      already created in V16
--   - idx_spare_parts_low_stock       already created in V16
--   - idx_wo_archived                 already created in V17
--   - idx_eh_logs_vehicle_date        already created in V12
--   - idx_notifications_recipient     already created in V20

-- Work Orders: branch + status + created_at (the most common filter; SRS §5.7).
CREATE INDEX IF NOT EXISTS idx_work_orders_branch_status
    ON ces_service.work_orders(branch_id, status, created_at DESC)
    WHERE deleted_at IS NULL;

-- Vehicles: branch + vehicle_type (SRS §5.7).
CREATE INDEX IF NOT EXISTS idx_vehicles_branch_type
    ON ces_service.vehicles(branch_id, vehicle_type)
    WHERE deleted_at IS NULL;

-- Service Requests: branch + status (frequent list filter).
CREATE INDEX IF NOT EXISTS idx_service_requests_branch_status
    ON ces_service.service_requests(branch_id, status, created_at DESC)
    WHERE deleted_at IS NULL;

-- Customers: branch + full_name lookups (list + search).
CREATE INDEX IF NOT EXISTS idx_customers_branch_name
    ON ces_service.customers(branch_id, full_name)
    WHERE deleted_at IS NULL;

-- Inspections: branch + status (due/in-progress lookups).
CREATE INDEX IF NOT EXISTS idx_inspections_branch_status
    ON ces_service.inspections(branch_id, status, inspection_date DESC)
    WHERE deleted_at IS NULL;

-- Spare part usages: work order lookups (cost reconstruction per WO).
CREATE INDEX IF NOT EXISTS idx_spare_part_usages_wo
    ON ces_service.spare_part_usages(wo_id);

-- WO cost items: work order lookups (cost summary per WO).
CREATE INDEX IF NOT EXISTS idx_wo_cost_items_wo
    ON ces_service.wo_cost_items(wo_id);
