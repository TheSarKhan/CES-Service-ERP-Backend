# Anbar modulu — genişlənmə planı

Bu sənəd 2026-avqust genişlənməsinin qərarlarını və mərhələlərini saxlayır. Məqsəd: hansı
seçimin niyə edildiyi kodun özündən görünməyəndə burada qalsın.

## Verilmiş qərarlar

| Mövzu | Qərar | Səbəb |
|---|---|---|
| Multi-location | `inventory_stock (məhsul, qovluq, miqdar)` — məhsul kataloq qeydidir | Eyni məhsul bir neçə rəfdə olur; «cəmi neçə var» sualının bir cavabı olmalıdır |
| Qalıq həqiqəti | Seriyasızda `inventory_stock`, seriyalıda vahidlər (stok sətri onlardan yenidən hesablanır) | İki mənbə gec-tez bir-birini tutmur |
| Hərəkət jurnalı | `inventory_stock_movements`, dəyişməz sətirlər | Tarixçə, köçürmə və sayım düzəlişlərinin təməli |
| Minimum stok | Məhsul üzrə, **ümumi qalıqla** müqayisə | «Sifariş verim?» sualı məhsul haqqındadır, rəf haqqında deyil |
| ~~İnventarizasiya~~ | **Ləğv edildi** (V45) — sayım düzəlişi məhsul kartındakı «Sayım» düyməsində qalır | Bax: «İnventarizasiya niyə atıldı» |
| ~~Transfer~~ | **Ləğv edildi** (V44) — qovluqlar arası köçürmə üçün məhsul kartındakı «Köçür» qalır | Bax: «Transfer niyə atıldı» |
| Lot | Lot-lar ayrıca, **FEFO təklif olunur** (məcbur edilmir) | Köhnə partiya anbarda qalıb bitməsin, amma istisna hallar bağlanmasın |
| Xəbərdarlıq | Sistem daxili + **gündəlik email** (Gmail SMTP) | |
| Email alıcıları | **Əl ilə ünvan siyahısı**, Anbar Konfiqurasiyadan | |
| Hərəkət tarixçəsi | **Sıfırdan başlayır**, audit jurnalından köçürülmür | Köhnə qeydlər natamamdır; natamam tarixçə həqiqət kimi görünər |

### Qərar verilməmiş, mənim seçdiyim
- **Son istifadə xəbərdarlığı**: zəmanətdəki kimi 30 gün, məhsulda ayrıca sahə ilə dəyişdirilə bilər.
- **Lot izlənməsi**: məhsulda `is_lot_tracked` bayrağı — `is_serialized` ilə eyni anda seçilə bilməz
  (məhsul ya seriyalı, ya lotlu, ya adi olur).

## Mərhələlər

| # | Mərhələ | Vəziyyət |
|---|---|---|
| 0 | Hərəkət jurnalı (`inventory_stock_movements`, `StockLedger`) | ✅ V39 |
| 1 | Multi-location (`inventory_stock`) + bütün UI | ✅ V38 |
| 2 | Hərəkət tarixçəsi UI (məhsul kartında tab) | ✅ |
| 3 | Minimum/kritik stok, xəbərdarlıqlar, gündəlik email | ✅ V40 |
| 4 | ~~Transfer (göndər → yolda → qəbul)~~ | ⛔ V41-də quruldu, V44-də ləğv edildi |
| 5 | ~~İnventarizasiya (kor sayım, bir təsdiqlə tətbiq)~~ | ⛔ V42-də quruldu, V45-də ləğv edildi |
| 6 | Lot / son istifadə tarixi / FEFO | ✅ V43 |

Növbəti addım: SMTP açarını `.env`-ə yazmaq və serverə deploy.

## Partiyalar niyə Zəmanətdən çıxarıldı

Əvvəlcə «Bitmək üzrə partiyalar» Zəmanət səhifəsində üçüncü tab idi. Məntiq belə idi: hər ikisi
*«bir şey tarixə görə yararsız olur»* deyir.

İstifadəçi üçün işləmədi — və haqlı idi. **«Zəmanət» başlığı yağın xarab olmasını vəd etmir.**
Başlıq nə deyirsə, içində o olmalıdır.

İndi Anbar səhifəsindəki diqqət zolağındadır, «Kritik həddən aşağı» və «Minimum həddən aşağı»
kartlarının yanında. Üçü də eyni cinsdən siqnaldır: *bu gün nəyəsə baxmaq lazımdır*.

İki detal:

- Zolağın saydığı üfüq (`BAND_HORIZON_DAYS = 90`) siyahının standart üfüqü ilə **eyni olmalıdır**.
  Kliklədiyin rəqəmlə açılan siyahının sayı fərqlənirsə, rəqəm heç olmamaqdan pisdir.
  30 gün bir partiyanı «təcili» işarələmək üçün doğrudur, amma zolağın verdiyi sual başqadır —
  *bu rüb nəyi planlamalıyam* — və yağ sifarişi 30 gündən uzun çəkir.
- Partiya sətrində `WarrantyStatusBadge` istifadə olunurdu və mühərrik yağının üzərinə
  **«Zəmanətli»** yazırdı. Ayrıca `ExpiryStatusBadge` yazıldı: *Yararlı / Bitməkdə / Vaxtı keçib*.
  Eyni rənglər, düzgün sözlər.

Partiya sayları `StockAlertSummaryResponse`-a əlavə edildi, amma `total`-a **qatılmadı** — o rəqəm
yan menyudakı nişanı idarə edir və məhsul sayır; partiyaları səssizcə ora qatmaq ekranda artıq
duran bir rəqəmin mənasını dəyişərdi.

## İnventarizasiya niyə atıldı

Vərəq mexanizmi kor sayım idi: sayan adam sistem rəqəmini görmür, bütöv qovluq bir sessiyada
sayılır, fərqlərin hamısı bir təsdiqlə tətbiq olunur.

Yerində qalan: məhsul kartındakı **«Sayım»** düyməsi — `POST /inventory/items/{id}/adjust`,
`StockLedger.setAbsolute` ilə mütləq rəqəm yazır, jurnala `ADJUST` sətri düşür, təsdiq
növbəsindən keçir.

**İtirilən iki şey, açıq deyilir:**

1. **Kor sayım.** Kart dialoqunda cari qalıq göz önündədir. «105 görüb 105 yazmaq» riski geri
   qayıdır — vərəqin mövcud olma səbəbi məhz bu idi.
2. **Sessiya.** Bütöv rəfi saymaq üçün hər məhsul ayrıca açılır; «bu qovluqda nə sayılmayıb»
   sualının cavabı yoxdur.

Qərar şüurlu verildi: bir otaqlı anbarda ayrıca modul saxlamaq bu iki faydaya dəymirdi.

`approval_requests`-dəki `STOCKTAKE_APPLY` sətirləri **silinmir** — baş vermiş qərarlardır.
Ona görə `ApprovalOperation.STOCKTAKE_APPLY` və `ApprovalEntityType.INVENTORY_STOCKTAKE`
enum-ları da qalır: silinsə, təsdiq tarixçəsi ümumiyyətlə yüklənməzdi.

`ApprovalExecutor.onNotApplied` tamamilə çıxarıldı — yeganə implementasiyası sayım vərəqi idi.

## Transfer niyə atıldı

Modul iki addımlı idi: göndər → «yolda» → qəbul et. Bütün dəyəri malın rəfdən çıxdığı və
təyinata çatdığı anlar arasındakı **müddəti** qeyd etməkdə idi — sayım vaxtı «bu 100 metr
haradadır?» sualına cavab verirdi.

Bir otaqlı anbarda o müddət saniyələrlə ölçülür. Yəni qeyd etməyə dəyər bir şey yoxdur, amma
hər köçürməyə ikinci düymə basmaq borcu qalır — həll etdiyindən çox iş yaradan modul.

Yerində qalan: **`POST /inventory/items/{id}/move`** — təsdiq növbəsindən keçir, jurnala
`TRANSFER_OUT` + `TRANSFER_IN` cütü yazır.

### Qismən köçürmə

`MoveItemRequest.quantity` opsionaldır:

- **boş** → qovluqdakı bütün qalıq gedir, mənbə stok sətri bağlanır (məhsul orada qalmır);
- **rəqəm** → qalıq bölünür, mənbə sətri **yaşayır** — məhsul hər iki qovluqda olur.

İki qayda kodda bir yerdə saxlanılır (`resolveMoveAmount`), çünki sorğu **parklanmazdan əvvəl**
(`assertMovable`) və təsdiqləndikdən **sonra** eyni şeyi rədd etməlidir. İlk versiyada yalnız
təsdiq anında yoxlanılırdı: 999999 ədədlik sorğu növbəyə düşür və heç vaxt təsdiqlənə bilməyəcəyi
halda məhsulu kilidləyirdi — bu, 3-cü qaydanın pozulması idi.

Seriyalı məhsulda miqdar göndərmək `ITEM_IS_SERIALIZED` ilə rədd olunur: orada qalıq vahidlərdən
yenidən hesablanır, ona görə əl ilə bölünmüş rəqəm ilk vahid dəyişikliyində üstündən yazılardı.

Modul geri qaytarılarsa: V41 faylı repo-da qalıb (Flyway tətbiq edilmiş miqrasiyanın yoxa
çıxmasını xəta sayır), silinən Java sinifləri git tarixçəsindədir.

## Qaydalar (kod yazarkən pozulmamalı)

1. **Qalığa yalnız `StockLedger` toxunur.** Servis birbaşa `inventory_stock`-a yazsa, jurnalda
   deşik qalır — deşikli tarixçə heç tarixçə olmamaqdan pisdir, çünki inandırıcı görünür.
2. **Jurnal sətri dəyişdirilmir, silinmir.** Səhv hərəkət əks hərəkətlə bağlanır.
3. **Qovluğa mal gətirmək = məhsulu ora yerləşdirmək**, ona görə qovluğun kateqoriya qaydası
   işləyir və bu, sorğu veriləndə yoxlanılır — təsdiq anında yox. Gözləyən sorğu məhsulu kilidləyir;
   heç vaxt uğur qazanmayacaq sorğu parklamaq məhsulu bloklayır.
4. **Təsdiq bildirişi şərti render olunan dialoqun içində yaşamamalıdır** — bağlananda unmount olur
   və istifadəçi heç bir cavab görmür.
5. **Təsdiqi rədd edilə bilən əməliyyat özünü gözləmə vəziyyətinə salırsa**, `ApprovalExecutor.onNotApplied`
   ilə oradan çıxarılmalıdır. Əks halda qeyd əbədi kilidli qalır (sayım vərəqi belə idi).
6. **Azərbaycan mətnini JS `toLowerCase()` ilə müqayisə etməyin** — `'SAYILAN'.toLowerCase()`
   `sayilan` verir, `sayılan` yox. Testlərdə də bu tələyə düşdük.

## SMTP

Gmail / Google Workspace seçilib. Lazım olan dəyişənlər (boş qaldıqda email göndərilmir, qalan
hər şey işləyir):

```
CES_MAIL_HOST=smtp.gmail.com
CES_MAIL_PORT=587
CES_MAIL_USERNAME=<ünvan>
CES_MAIL_PASSWORD=<app password>
```

Alıcılar bazada saxlanılır (`inventory_settings.notification_emails`), Anbar Konfiqurasiya →
Tənzimləmələr bölməsindən idarə olunur.
