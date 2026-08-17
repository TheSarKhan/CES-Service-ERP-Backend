-- V55__backfill_vehicle_primary_photo.sql
-- Photos uploaded before VehiclePhotoService.upload() started auto-assigning a cover photo never
-- got one — vehicles.primary_photo_id/primary_photo_url stayed NULL even though real photos
-- exist, so the Texnikalar list thumbnail (and anything else reading primaryPhotoUrl) shows the
-- placeholder icon forever for those vehicles. Backfills the earliest-uploaded photo as the
-- cover, matching the app's "first photo becomes primary" behavior going forward.

UPDATE ces_service.vehicles v
SET primary_photo_id  = p.id,
    primary_photo_url = p.file_url
FROM (
    SELECT DISTINCT ON (vehicle_id) vehicle_id, id, file_url
    FROM ces_service.vehicle_photos
    WHERE deleted_at IS NULL
    ORDER BY vehicle_id, created_at ASC
) p
WHERE v.id = p.vehicle_id
  AND v.primary_photo_id IS NULL;
