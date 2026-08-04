-- V44__drop_inventory_transfers.sql
-- Transfer modulu ləğv edilir. Qovluqlar arası köçürmə üçün məhsul kartındakı «Köçür»
-- (POST /inventory/items/{id}/move) kifayət edir: o, bir qovluqdakı bütün qalığı başqasına
-- keçirir və təsdiq növbəsindən keçir.
--
-- Niyə iki addımlı transfer atıldı: onun bütün dəyəri malın «yolda» olduğu müddəti qeyd
-- etməkdə idi. Bir otaqlı anbarda o müddət saniyələrlə ölçülür, yəni qazanc yoxdur, amma hər
-- köçürməyə ikinci bir düymə basmaq borcu qalır.
--
-- V41 faylı silinmir: Flyway artıq tətbiq edilmiş miqrasiyanın yoxa çıxmasını xəta sayır.
-- Cədvəllər isə burada silinir ki, kodu olmayan boş cədvəl bazada qalıb çaşdırmasın.

DROP TABLE IF EXISTS ces_service.inventory_transfer_lines;
DROP TABLE IF EXISTS ces_service.inventory_transfers;

-- Bu tənzimləmə yalnız transfer qəbulunu idarə edirdi — «göndərəni qəbul edən eyni adam
-- olmasın» qaydası. Köçürmədə belə bir ikinci addım yoxdur, ona görə sütun da mənasızdır.
ALTER TABLE ces_service.inventory_settings
    DROP COLUMN IF EXISTS transfer_requires_different_receiver;
