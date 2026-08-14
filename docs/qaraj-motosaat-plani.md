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

## Motosaat sxemi (V47)

`engine_hour_logs` → `meter_readings` adına keçdi, `meter_type` sütunu əlavə olundu
(`ENGINE_HOURS`/`KILOMETERS`), `hours_value` → `value`, `entry_type` → `source` (sərt CHECK
yerinə `garage_config_values(METER_SOURCE)`-a qarşı yoxlanılır — V46-da artıq is_system=TRUE
seed edilib: Manual/Servis/Baxım/İdxal/Digər). `deleted_at` əlavə olundu (BaseEntity tələbi,
Qarajda tapılan eyni tələ). Trigger `meter_type`-a görə `vehicles.current_engine_hours` və ya
`current_km`-i yeniləyir.

**Reset/rollover** iki ayrı endpoint kimi qurulub, bir-birinin fallback-ı deyil:
- `POST /vehicles/{id}/meter-readings` — normal qeyd, cari dəyərdən aşağı olarsa **birbaşa rədd
  olunur** (`ENGINE_HOURS_DECREASING`), heç bir təsdiq axınına düşmür.
- `POST /vehicles/{id}/meter-readings/rollover` — bilərəkdən reset, **məcburi səbəb**, həmişə
  Təsdiqləmələr növbəsindən keçir (`ApprovalEntityType.METER_READING`, `ApprovalOperation
  .METER_ROLLOVER`, `entityId` = vehicleId — texnikanı kilidləyir, oxşar bir rollover isteği
  gözləyərkən ikincisi qəbul olunmur).

**Yeni cədvəllər:**
- `vehicle_maintenance_plans` — texnika üzrə fərdi baxım sətri, `garage_maintenance_
  template_items`-in sütunlarını (interval_meter_hours/interval_km/interval_calendar_days)
  eynilə güzgülər, FK yox (şablon dəyişikliyi mövcud plana keçməsin — brif). Plan yaradılanda
  `last_done_*` texnikanın CƏLB OLUNAN meter tipi üzrə CARİ dəyərinə/tarixinə "başlanğıc nöqtəsi"
  kimi doldurulur — plan sıfırdan "indi başlayır".
- `vehicle_maintenance_completions` — "Baxımı tamamla" (yalnız Manual yol; "Servis vasitəsilə"
  Servis modulu qurulanda əlavə olunacaq). Tamamlanma zamanı verilən motosaat/KM dəyərləri EYNİ
  zamanda adi bir `meter_readings` sətri kimi də yazılır (`source='Baxım'`) — vahid tarixçə,
  iki yerdə saxlanmır.

**İcazə:** yeni ad uydurulmayıb — V4-də artıq olan `EH_READ`/`EH_CREATE` bütün Motosaat
əməliyyatlarını (qeyd, plan CRUD, tamamlama, şablon tətbiqi) örtür. Rollover-in təsdiqi isə
(V49-dan sonra) `GARAGE_APPROVAL_READ`/`GARAGE_APPROVAL_DECIDE` ilə idarə olunur — bax aşağıdakı
"Qaraj təsdiq növbəsi Anbardan ayrıldı" bölməsi.

**Gələcəyə saxlanılanlar** (brifdə var, bu mərhələdə tikilmədi, çünki asılı olduğu modul yoxdur):
- "Servis vasitəsilə" tamamlanma yolu — Servis modulu (M05/M06) qurulanda
  `vehicle_maintenance_completions`-a `source_ref_id`-oxşar sütun əlavə olunacaq.
- `engine_hour_alerts` (Baxış modulu, M18) — toxunulmadı, ayrı konsepsiya (bax yuxarı bölmə).

## Material/ehtiyat hissələri Anbar ilə əlaqələndirilməsi (V48)

Baxım tamamlanmasında istifadə olunan materiallar indi Anbar/Stok ilə əlaqələndirilə bilər —
əvvəlki mərhələdə "gələcəyə saxlanılan" bu maddə artıq bağlıdır. `materialsNotes` (sərbəst mətn)
saxlanılır, əlavə olaraq strukturlaşdırılmış `materials` siyahısı gəlir (istifadəçinin qərarı:
hər ikisi olsun, biri digərini əvəz etməsin):

- **CONSUMABLE** (sayıla bilən — yağ, filtr): Anbar item + qovluq + miqdar seçilir, adi bir
  Anbar stok-çıxışı (`STOCK_OUT`) təqdim olunur. Bu, **Anbarın öz Təsdiqləmələr növbəsindən**
  keçir — baxımın özündən tam müstəqil (istifadəçinin qərarı: bax aşağı). Baxım dərhal
  tamamlanır; stok-çıxışı ayrıca təsdiq gözləyir.
- **SERIALIZED** (seriya nömrəli hissə — hidravlik nasos və s.): seçilən `InventoryItemUnit`
  birbaşa `IN_USE` statusuna keçir (vahid statusu dəyişikliyi Anbarda təsdiq tələb etmir) VƏ
  eyni zamanda Qarajın `vehicle_components`-də yeni ACTIVE sətir kimi əks olunur (istifadəçinin
  qərarı: avtomatik sinxron olsun) — Komponentlər tabı əl ilə ayrıca doldurulmasın deyə.

Yeni cədvəl: `vehicle_maintenance_completion_materials` — hər sətir CONSUMABLE ya SERIALIZED
sahələrini daşıyır (DB CHECK ilə qarşılıqlı təmin olunur), `inventoryItemName`/`unit`/
`serialNumber` yazma anında snapshot olunur ki, mənbə sətir sonra dəyişsə/silinsə də oxunaqlı
qalsın.

## Qaraj təsdiq növbəsi Anbardan ayrıldı (V49)

İstifadəçinin açıq qərarı: Qaraj/Motosaat təsdiqləri (VEHICLE, METER_READING) Anbarın
`approval_requests` cədvəlində qarışmamalıdır — həqiqi ayrı baza cədvəli və ayrı sidebar
modulu/səhifəsi olmalıdır (Qaraj altında, Anbarın öz "Təsdiqləmələr" ekranı ilə paylaşılmadan).

- Yeni cədvəl: `garage_approval_requests` (`approval_requests` ilə eyni struktur). Miqrasiya
  mövcud VEHICLE/METER_READING sətirlərini köhnə cədvəldən köçürür və oradan silir — tarixçə
  itmir.
- Yeni Java paketi: `module.garageapproval` — `GarageApprovalRequest`/`GarageApprovalService`/
  `GarageApprovalExecutor`/`GarageApprovalController`, Anbarın `module.approval`-ın tam
  müstəqil əkizi (`ApprovalStatus`/`ApprovalDecisionRequest` ümumi olduğu üçün paylaşılır,
  entity-type/operation enumları isə ayrıdır: `GarageApprovalEntityType`
  {VEHICLE, METER_READING}, `GarageApprovalOperation` {UPDATE, DELETE, METER_ROLLOVER}).
- `VehicleController`/`MeterReadingController` və onların executor-ları `GarageApprovalService`-ə
  köçürüldü; köhnə `ApprovalEntityType`-dan VEHICLE/METER_READING, `ApprovalOperation`-dan
  METER_ROLLOVER silindi.
- Yeni icazələr: `GARAGE_APPROVAL_READ`/`GARAGE_APPROVAL_DECIDE` (Anbarın APPROVAL_READ/DECIDE-i
  ilə eyni rol bölgüsü — Admin/Servis Meneceri/Direktor qərar verə bilir).
- Frontend: `/garage/approvals` (yeni səhifə, Qaraj sidebar qrupunun altında, "Qaraj
  Təsdiqləmələr" adı ilə), `components/garage/GarageApprovalPanel.tsx` — Anbarın
  `ApprovalPanel.tsx`-in sadələşdirilmiş əkizi (stok/köçürmə diff blokları yoxdur, hər əməliyyat
  sadə sahə diff-idir).
- **Fərq:** material tamamlanmasında CONSUMABLE sətirinin göndərdiyi Anbar stok-çıxışı **Anbarın
  öz növbəsində qalır** — bu, Qarajdan tətiklənsə də, həqiqətən Anbar hərəkətidir (istifadəçinin
  açıq qərarı). Yalnız Qarajın/Motosaatın öz təbiətli əməliyyatları (Texnika redaktəsi/silinməsi,
  Motosaat sıfırlanması) yeni növbəyə keçdi.
- Uc-uca curl testi ilə təsdiqləndi: VEHICLE/METER_READING yalnız `/garage/approvals`-da görünür,
  `/approvals`-a sızmır; stok-çıxış təsdiqi əksinə, yalnız `/approvals`-da qalır və
  `/garage/approvals`-a sızmır.

## Backend doğrulaması

Təcrid olunmuş QA mühitində (55432/58081/3001): V47 miqrasiyası təmiz tətbiq olundu, Hibernate
sxem yoxlaması keçdi, 21 ssenarili curl testi (qeyd, rollover+approval, plan yaratma, due/overdue
hesablanması, tamamlama, şablon tətbiqi, deaktivasiya, silmə) hamısı gözlənilən nəticəni verdi.

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
| 1 | Qaraj: baza sxemi | ✅ |
| 2 | Qaraj: backend (CRUD, kod generasiyası, təsdiq) | ✅ |
| 3 | Qaraj: Konfiqurasiya API | ✅ |
| 4 | Qaraj: frontend | ✅ |
| 5 | Motosaat: baza sxemi (V47) | ✅ |
| 6 | Motosaat: backend (qeyd, reset, baxım planı) | ✅ |
| 7 | Motosaat: frontend | ✅ |
| 8 | Material/ehtiyat hissələrinin Anbar ilə əlaqələndirilməsi (V48) | ✅ |
| 9 | Qaraj təsdiq növbəsinin Anbardan ayrılması (V49) | ✅ |
