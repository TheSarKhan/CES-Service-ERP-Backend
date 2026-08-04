# Anbar modulu — genişlənmə planı

Bu sənəd 2026-avqust genişlənməsinin qərarlarını və mərhələlərini saxlayır. Məqsəd: hansı
seçimin niyə edildiyi kodun özündən görünməyəndə burada qalsın.

## Verilmiş qərarlar

| Mövzu | Qərar | Səbəb |
|---|---|---|
| Multi-location | `inventory_stock (məhsul, qovluq, miqdar)` — məhsul kataloq qeydidir | Eyni məhsul bir neçə rəfdə olur; «cəmi neçə var» sualının bir cavabı olmalıdır |
| Qalıq həqiqəti | Seriyasızda `inventory_stock`, seriyalıda vahidlər (stok sətri onlardan yenidən hesablanır) | İki mənbə gec-tez bir-birini tutmur |
| Hərəkət jurnalı | `inventory_stock_movements`, dəyişməz sətirlər | Tarixçə, transfer və sayım fərqlərinin təməli |
| Minimum stok | Məhsul üzrə, **ümumi qalıqla** müqayisə | «Sifariş verim?» sualı məhsul haqqındadır, rəf haqqında deyil |
| İnventarizasiya | **Kor sayım** — sayan adam sistem miqdarını görmür | Açıq sayımda adam rəqəmi yoxlamadan təsdiqləyir |
| Sayım fərqləri | Bütöv sessiya üçün **bir təsdiq** | Paralel təsdiq qorunur, 200 sətri tək-tək təsdiqləmək məcburiyyəti olmur |
| Transfer | **İki addım**: göndər → yolda → qəbul et | Fiziki köçürmə vaxt aparır; mal heç bir yerdə görünməyən vaxt olmamalıdır |
| Transfer qəbulu | Qəbul edən göndərəndən fərqli olmalıdır — **filial üzrə açılıb-bağlanan** | Böyük filialda nəzarət, kiçikdə blok olmasın |
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
| 4 | Transfer (göndər → yolda → qəbul) | ✅ V41 |
| 5 | İnventarizasiya (kor sayım, bir təsdiqlə tətbiq) | ✅ V42 |
| 6 | Lot / son istifadə tarixi / FEFO | ✅ V43 |

Hamısı bitib. Növbəti addım: SMTP açarını `.env`-ə yazmaq və serverə deploy.

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
