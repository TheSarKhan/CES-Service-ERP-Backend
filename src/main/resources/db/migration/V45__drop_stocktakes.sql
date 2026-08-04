-- V45__drop_stocktakes.sql
-- İnventarizasiya vərəqi ləğv edilir. Sayım düzəlişi məhsul kartındakı «Sayım» düyməsində qalır
-- (POST /inventory/items/{id}/stock-adjust) — bir məhsul, bir qovluq, təsdiq növbəsindən keçir,
-- jurnala ADJUST sətri yazır.
--
-- Nə itir: kor sayım (sayan adam sistem rəqəmini görmür) və bütöv qovluğun bir sessiyada
-- sayılması. Kart düyməsində cari qalıq göz önündədir və hər məhsul ayrıca açılır.
--
-- V42 faylı silinmir: Flyway artıq tətbiq edilmiş miqrasiyanın yoxa çıxmasını xəta sayır.
--
-- approval_requests-dəki STOCKTAKE_APPLY sətirləri SAXLANILIR. Onlar baş vermiş qərarlardır və
-- silinsə təsdiq tarixçəsində deşik qalar; entity_id artıq mövcud olmayan sətrə baxır, amma
-- ekranda göstərilən ad sorğunun öz sütunundadır, ona görə siyahı normal render olunur.

DROP TABLE IF EXISTS ces_service.inventory_stocktake_lines;
DROP TABLE IF EXISTS ces_service.inventory_stocktakes;
