-- V33__backfill_inventory_qr_codes.sql
-- Purpose: V32 seed-i node/item/unit sətirlərini birbaşa SQL ilə yazdığı üçün servis
-- qatından yan keçdi və qr_code / barcode sütunları NULL qaldı. Nəticədə həmin obyektlərin
-- QR dialoqu «Bu obyekt üçün QR kod yoxdur» göstərirdi.
--
-- Servis qatı bu kodları UUID kimi yaradır (InventoryNodeService.generateCode /
-- InventoryItemService.create), ona görə burada da eyni format istifadə olunur.
-- Yalnız NULL olanlar doldurulur — mövcud kodlara toxunulmur, çünki onlar artıq
-- çap edilmiş etiketlərdə ola bilər.

UPDATE ces_service.inventory_nodes
SET qr_code = gen_random_uuid()::text
WHERE qr_code IS NULL;

UPDATE ces_service.inventory_nodes
SET barcode = gen_random_uuid()::text
WHERE barcode IS NULL;

UPDATE ces_service.inventory_items
SET qr_code = gen_random_uuid()::text
WHERE qr_code IS NULL;

UPDATE ces_service.inventory_item_units
SET qr_code = gen_random_uuid()::text
WHERE qr_code IS NULL;
