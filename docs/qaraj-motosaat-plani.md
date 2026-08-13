# Qaraj + Motosaat modulları — plan

Bu sənəd 2026-avqust Qaraj/Motosaat qurulmasının qərarlarını saxlayır. Mənbələr:
`CES Service Qaraj Modulu.pdf`, `CES Service Motosaat Modulu.pdf` (product brifləri) və
`backend/docs/CES_Service_SRS_v1.0.pdf` (M03/M08 — orijinal texniki spesifikasiya, bazadakı
skelet ondan gəlir).

## Başlanğıc nöqtəsi: boş skelet deyil

Nəqliyyat modulu silinərkən (əvvəlki sessiya) `vehicles`, `vehicle_documents`,
`engine_hour_logs`, `engine_hour_alerts`, `service_requests`, `work_orders`, `inspections`
cədvəlləri **bilərəkdən saxlanılıb** — SRS-in "bu modullar üçün skelet" şərhi ilə. Java kodu
yoxdur, baza var. Sıfırdan yox, mövcud sxemi brifə uyğun yenidən formalaşdırırıq.

## SRS ilə brif arasında fərqlər — brif üstün tutulub

| Mövzu | SRS (M03/M08) | Brif | Qərar |
|---|---|---|---|
| Sahiblik | 3 dəyər: COMPANY/INVESTOR/CUSTOMER | 2 dəyər: Company/Customer Owned | **Brif** — investor fərqi gələcək Müştərilər moduluna saxlanılır |
| Motosaat sxemi | `hours_value` sabit sütun | Ümumiləşdirilmiş `Meter Type` (ENGINE_HOURS/KILOMETERS) | **Brif** — KM üçün baza yenidən qurulmasın |
| Motosaat redaktəsi | Endpoint yoxdur | Redaktə/reset Təsdiqləmələr növbəsindən keçməli | **Brif** (SRS-i genişləndirir, ziddiyyət yoxdur) |
| Şassi/Seriya/Qeydiyyat unikallığı | Yalnız şassi, bütün filiallar üzrə | Üçü də unikal | **Brif genişləndirir**, "bütün filiallar üzrə" qaydası **hər üçünə** tətbiq olunur (SRS-in bu detalı düzgündür, saxlanılır) |

## Verilmiş qərarlar (istifadəçi ilə)

| Mövzu | Qərar | Səbəb |
|---|---|---|
| Məcburi sahələr | Ad, Növ, Marka, Model məcburi; Seriya/Şassi/Qeydiyyat könüllü (daxil edilsə unikal) | Real texnikada (ekskavator) dövlət qeydiyyatı olmur — məcburi etsək qeydiyyatı bloklayır |
| Konfiqurasiya | Tam idarəetmə səhifəsi indi qurulur (Anbar kateqoriyaları kimi) | İstifadəçi seçimi — uzunmüddətli rahatlıq üçün əlavə iş qəbul edildi |
| Təsdiq (Qaraj) | Texnika redaktəsi/status/sahib dəyişikliyi Təsdiqləmələr növbəsindən keçir | İstifadəçi seçimi — Motosaatla eyni sərtlik səviyyəsi |
| Baxımın yeri | Baxım Motosaat ekranında, texnikaya klikləyərək əlçatandır | İstifadəçinin UI axını qərarı — brifin "Motosaat baxımı idarə etməməlidir" qeydi ilə ziddiyyət deyil, o məsuliyyət bölgüsüdür (ayrı cədvəl/servis), bu isə ekran yerləşməsidir |

## Qaraj ≠ Baxış (Inspection)

Bazada ayrıca, hələ tikilməmiş bir **Baxış (Inspection, M18)** modulu da planlaşdırılıb —
`inspection_schedules`/`inspection_checklists`, checklist-əsaslı texniki müayinə (M18 SRS:
"Şirkət Qarajındakı texnikaların dövri texniki baxışı"). Bu, brifin "Baxım" (yağ dəyişimi və s.)
konsepsiyasından **fərqlidir** — struktur oxşardır (interval + növbəti tarix hesablanması) amma
biznes mənası ayrıdır. Bu iki anlayışı qarışdırmırıq: Baxım Motosaat daxilində öz cədvəllərini
alır, Baxış modulu toxunulmadan qalır, gələcəkdə ayrıca tikiləcək.

`engine_hour_alerts` cədvəli də Baxışa aiddir (`alert_type IN ('INSPECTION_DUE','CUSTOM')`) —
Motosaat mərhələsində ona toxunmuruq.

## İcazələr — yenisi uydurulmur

V4 miqrasiyasında artıq var: `VEHICLE_READ/CREATE/UPDATE/DELETE/OVERRIDE` (Qaraj),
`EH_READ/CREATE/EH_ALERT_MANAGE` (Motosaat). Bunları genişləndiririk, yeni ad uydurmuruq.
`VEHICLE_OVERRIDE` brifin "səlahiyyətli istifadəçi IN_SERVICE xəbərdarlığını keçə bilər"
qaydası üçün artıq hazırdır (Servis modulu qurulanda istifadə olunacaq).

## Qaraj sxemi (bu mərhələdə)

**`vehicles`** dəyişiklikləri:
- `code` (TEX-000001, sequence+trigger, dəyişdirilə bilməz)
- `name` (məcburi, istifadəçi daxil edir)
- `garage_type` CHECK → yalnız `COMPANY`/`CUSTOMER`
- `owner_id` → `customers(id)` FK əlavə olunur (cədvəl artıq var, boş saxlamaq mənasızdır)
- `status` → CHECK constraint yox, sərbəst mətn, `garage_config_values(list_type='STATUS')`-a
  qarşı servis qatında yoxlanılır (Konfiqurasiya qərarına görə — sabit enum ola bilməz, çünki
  admin yeni status əlavə edə bilməlidir)
- `uses_engine_hours`, `uses_km` (bool, texnika növünün defaultundan kopyalanır, sonra sərbəst)
- `current_km`, `last_km_at` (sürətli oxuma keşi — Motosaat mərhələsində doldurulacaq)
- şassi/seriya/qeydiyyat: partial unique index-ə keçirilir (`WHERE deleted_at IS NULL`) —
  soft-silinmiş texnikanın identifikasiya nömrəsi təkrar istifadə oluna bilsin

**Yeni cədvəllər:**
- `vehicle_photos` — kateqoriyalı fotolar
- `vehicle_components` — status-əsaslı tarixçə (ACTIVE/REMOVED), ayrıca history cədvəli yox
  (InventoryItemUnit-in vahid statusu naxışı)
- `garage_config_values` — bütün açılan siyahılar üçün **tək** cədvəl: növ, marka, model,
  status, lokasiya, sənəd növü, foto kateqoriyası, komponent növü, baxım növü, mənbə.
  Anbarın tam EAV sistemi lazım deyil — Qaraj heç bir tipə görə dinamik SAHƏ tələb etmir,
  yalnız açılan siyahı DƏYƏRLƏRİ. `is_system` sıradan silinməyə qarşı qoruyur (yalnız STATUS
  və METER_SOURCE üçün əhəmiyyətlidir — digərləri sərbəst nümunədir).
- `garage_maintenance_templates` / `garage_maintenance_template_items` — texnika növü üzrə
  şablon (Konfiqurasiya hissəsi). Əməliyyat (texnikanın öz planı, gecikmə hesablanması)
  Motosaat mərhələsində.

Marka/Model/Növ/Lokasiya sərbəst mətn sahələridir, yazıldıqda `garage_config_values`-da
mövcud deyilsə avtomatik qeydə alınır (brifin "yeni marka gələcək qeydiyyatlarda seçim kimi
görünsün" tələbi) — FK yox, çünki sərbəst yaradılma tələb olunur.

## Motosaat sxemi (növbəti mərhələdə, qeyd üçün)

`engine_hour_logs` → `meter_readings` adına keçir, `meter_type` sütunu əlavə olunur
(`ENGINE_HOURS`/`KILOMETERS`), `hours_value` → `value`. Qalan sütunlar (previous_value,
generated delta, entry_type, is_rollover, rollover_reason, source_ref_id, recorded_at) olduğu
kimi qalır — SRS-in bu hissəsi düzgün düşünülüb, dəyişən yalnız ümumiləşdirmədir.

## Qaraj backend — tapılan iki tələ

1. **`vehicle_components` `deleted_at`-sız qalmışdı.** Hər entity `BaseEntity`-dən miras alır,
   o da `deletedAt` tələb edir — statusun (ACTIVE/REMOVED) özü "silinmə" demək deyil, komponent
   səhv texnikaya yazılıbsa, əsl silinmə yolu da lazımdır. Hibernate-in sxem yoxlaması bunu
   dərhal tutdu.
2. **`code` sahəsi cavabda `null` gəlirdi.** Trigger bazada düzgün `TEX-000001` yazır, amma
   Hibernate-in yaddaşdakı obyekti bunu bilmir — sahə `@Generated` ilə işarələnməlidir ki,
   INSERT-dən sonra geri oxunsun. Bu da kifayət etmədi: `@Generated` yalnız **faktiki INSERT
   bazaya çatanda** işləyir, JPA isə defolt olaraq yazını commit-ə qədər gecikdirir (write-behind).
   `save()`-i `saveAndFlush()`-a çevirmək düzəltdi. Hər ikisi API testində tutuldu, prod-a
   getməzdən əvvəl.

## Mərhələlər

| # | Mərhələ | Vəziyyət |
|---|---|---|
| 1 | Qaraj: baza sxemi | 🔄 |
| 2 | Qaraj: backend (CRUD, kod generasiyası, təsdiq) | ⏳ |
| 3 | Qaraj: Konfiqurasiya API | ⏳ |
| 4 | Qaraj: frontend | ⏳ |
| 5 | Motosaat: baza sxemi | ⏳ |
| 6 | Motosaat: backend (qeyd, reset, baxım planı) | ⏳ |
| 7 | Motosaat: frontend | ⏳ |
