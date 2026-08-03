-- V32__seed_inventory_demo.sql
-- Purpose: nümunə anbar məzmunu — struktur boş ikən modulun necə göründüyünü görmək üçün.
--
-- Ağac qəsdən DƏYİŞKƏN DƏRİNLİKDƏ qurulub, çünki real anbar belədir:
--   Skaf A > Ref A-1 > Qutu A-1-01 > Paket ...   → xırda bərkidicilər 4 səviyyə dərinlikdə
--   Skaf B > Ref B-1                             → motorlar 2 səviyyədə, qutu/paket YOXDUR
-- `inventory_items.node_id` istənilən dərinlikdəki node-a bağlana bildiyi üçün hər ikisi
-- eyni ağacda yaşayır; «sonuncu səviyyə» kimi sabit qayda yoxdur.
--
-- Kateqoriya ƏŞYANIN üzərindədir (inventory_items.category_id), yerin yox. Node-a bağlanan
-- kateqoriyalar (inventory_node_categories) yalnız «burada nə saxlanıla bilər» məhdudiyyətidir —
-- boş qoyulsa node sərbəstdir. Ona görə Ref-də duran motor heç nəyi sındırmır.
--
-- Qeyd: kateqoriya API üzərindən yaradılanda InventoryCategoryService 4 sistem sahəsi əlavə edir.
-- Bu seed SQL servisdən yan keçdiyi üçün həmin sahələr burada əl ilə, eyni ardıcıllıqla yazılır.

DO $$
DECLARE
    v_branch UUID := '11111111-1111-1111-1111-111111111111';
    v_user   UUID := '00000000-0000-0000-0000-000000000000';

    c_motor  UUID; c_berkidici UUID; c_filtr UUID; c_yag UUID; c_elektrik UUID;

    n_skaf_a UUID; n_ref_a1 UUID; n_ref_a2 UUID;
    n_qutu_a1_01 UUID; n_qutu_a1_02 UUID; n_paket_a1_01_01 UUID; n_paket_a1_01_02 UUID;
    n_skaf_b UUID; n_ref_b1 UUID; n_ref_b2 UUID;
    n_skaf_c UUID; n_ref_c1 UUID; n_qutu_c1_01 UUID;

    i_motor UUID; i_vint UUID; i_qayka UUID; i_filtr_yag UUID;
    i_hidravlik UUID; i_kabel UUID; i_klemma UUID;
BEGIN
    -- Guard yalnız BU seed-in öz sətirlərinə baxır, ümumi məzmuna yox: bazada artıq
    -- əl ilə yaradılmış kateqoriyalar ola bilər və nümunə onların yanında yaşamalıdır.
    -- «Heç nə yoxdursa işlə» qaydası olsaydı, bir dənə test kateqoriyası seed-i tamamilə
    -- bloklayardı.
    IF EXISTS (SELECT 1 FROM ces_service.inventory_categories
               WHERE branch_id = v_branch AND name = 'Mühərrik və aqreqat' AND deleted_at IS NULL) THEN
        RETURN;
    END IF;

    -- ── Kateqoriyalar ────────────────────────────────────────────────────────
    INSERT INTO ces_service.inventory_categories (branch_id, name, default_unit, created_by, updated_by)
    VALUES (v_branch, 'Mühərrik və aqreqat', 'ədəd', v_user, v_user) RETURNING id INTO c_motor;
    INSERT INTO ces_service.inventory_categories (branch_id, name, default_unit, created_by, updated_by)
    VALUES (v_branch, 'Bərkidici', 'ədəd', v_user, v_user) RETURNING id INTO c_berkidici;
    INSERT INTO ces_service.inventory_categories (branch_id, name, default_unit, created_by, updated_by)
    VALUES (v_branch, 'Filtr', 'ədəd', v_user, v_user) RETURNING id INTO c_filtr;
    INSERT INTO ces_service.inventory_categories (branch_id, name, default_unit, created_by, updated_by)
    VALUES (v_branch, 'Yağ və maye', 'litr', v_user, v_user) RETURNING id INTO c_yag;
    INSERT INTO ces_service.inventory_categories (branch_id, name, default_unit, created_by, updated_by)
    VALUES (v_branch, 'Elektrik', 'ədəd', v_user, v_user) RETURNING id INTO c_elektrik;

    -- Sistem sahələri — InventoryCategoryService.SYSTEM_FIELDS ilə eyni sıra və tip.
    INSERT INTO ces_service.inventory_category_fields
        (category_id, field_key, label, field_type, is_required, sort_order, is_visible, show_in_table, is_system)
    SELECT c.id, f.field_key, f.label, f.field_type, FALSE, f.sort_order, TRUE, f.show_in_table, TRUE
    FROM (VALUES (c_motor), (c_berkidici), (c_filtr), (c_yag), (c_elektrik)) AS c(id)
    CROSS JOIN (VALUES
        ('sekil',      'Şəkil',                  'IMAGE',    0, TRUE),
        ('aciqlama',   'Açıqlama',               'TEXTAREA', 1, FALSE),
        ('istehsalci', 'İstehsalçı / Təchizatçı','TEXT',     2, FALSE),
        ('veziyyet',   'Vəziyyət',               'TEXT',     3, FALSE)
    ) AS f(field_key, label, field_type, sort_order, show_in_table);

    -- Kateqoriyaya xas əlavə sahələr — dinamik sahə sisteminin nə üçün olduğunu göstərir.
    INSERT INTO ces_service.inventory_category_fields
        (category_id, field_key, label, field_type, is_required, sort_order, is_visible, show_in_table, is_system)
    VALUES
        (c_motor,     'guc_kw',    'Güc (kW)',        'NUMBER', FALSE, 4, TRUE, TRUE,  FALSE),
        (c_motor,     'dovr',      'Dövr (rpm)',      'NUMBER', FALSE, 5, TRUE, FALSE, FALSE),
        (c_berkidici, 'olcu',      'Ölçü (M)',        'TEXT',   FALSE, 4, TRUE, TRUE,  FALSE),
        (c_berkidici, 'uzunluq',   'Uzunluq (mm)',    'NUMBER', FALSE, 5, TRUE, FALSE, FALSE),
        (c_filtr,     'uygunluq',  'Uyğun texnika',   'TEXT',   FALSE, 4, TRUE, TRUE,  FALSE),
        (c_yag,       'viskozite', 'Viskozite',       'TEXT',   FALSE, 4, TRUE, TRUE,  FALSE),
        (c_elektrik,  'gerginlik', 'Gərginlik (V)',   'NUMBER', FALSE, 4, TRUE, TRUE,  FALSE);

    -- ── Yer ağacı ────────────────────────────────────────────────────────────
    -- Skaf A: xırda hissələr — dərinlik Paket səviyyəsinə qədər gedir.
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, NULL, 'Skaf A', 'A', v_user, v_user) RETURNING id INTO n_skaf_a;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_skaf_a, 'Ref A-1', 'A-1', v_user, v_user) RETURNING id INTO n_ref_a1;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_skaf_a, 'Ref A-2', 'A-2', v_user, v_user) RETURNING id INTO n_ref_a2;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_ref_a1, 'Qutu A-1-01', 'A-1-01', v_user, v_user) RETURNING id INTO n_qutu_a1_01;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_ref_a1, 'Qutu A-1-02', 'A-1-02', v_user, v_user) RETURNING id INTO n_qutu_a1_02;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_qutu_a1_01, 'Paket M8', 'A-1-01-01', v_user, v_user) RETURNING id INTO n_paket_a1_01_01;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_qutu_a1_01, 'Paket M10', 'A-1-01-02', v_user, v_user) RETURNING id INTO n_paket_a1_01_02;

    -- Skaf B: iri aqreqatlar — qutuya sığmır, Ref səviyyəsində dayanır.
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, NULL, 'Skaf B', 'B', v_user, v_user) RETURNING id INTO n_skaf_b;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_skaf_b, 'Ref B-1', 'B-1', v_user, v_user) RETURNING id INTO n_ref_b1;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_skaf_b, 'Ref B-2', 'B-2', v_user, v_user) RETURNING id INTO n_ref_b2;

    -- Skaf C: maye və filtrlər — qarışıq dərinlik.
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, NULL, 'Skaf C', 'C', v_user, v_user) RETURNING id INTO n_skaf_c;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_skaf_c, 'Ref C-1', 'C-1', v_user, v_user) RETURNING id INTO n_ref_c1;
    INSERT INTO ces_service.inventory_nodes (branch_id, parent_id, name, code, created_by, updated_by)
    VALUES (v_branch, n_ref_c1, 'Qutu C-1-01', 'C-1-01', v_user, v_user) RETURNING id INTO n_qutu_c1_01;

    -- Yer məhdudiyyəti: Ref B-1 yalnız mühərrik üçün ayrılıb. Qalan node-lar sərbəstdir
    -- (heç bir sətir = məhdudiyyət yoxdur), yəni bu, qayda deyil, könüllü daraltmadır.
    INSERT INTO ces_service.inventory_node_categories (node_id, category_id)
    VALUES (n_ref_b1, c_motor);

    -- ── Əşyalar ──────────────────────────────────────────────────────────────
    -- Motorlar: seriyalı izlənir (is_serialized), ona görə miqdar vahidlərdən gəlir.
    -- Diqqət: node_id = Ref B-1, yəni qutu/paket olmadan birbaşa rəfdə.
    INSERT INTO ces_service.inventory_items
        (branch_id, node_id, category_id, name, sku, unit, quantity, purchase_price,
         is_serialized, attributes, created_by, updated_by)
    VALUES (v_branch, n_ref_b1, c_motor, 'Elektrik mühərriki 7.5 kW', 'MTR-7K5', 'ədəd', 5, 1850.00,
            TRUE, '{"istehsalci":"WEG","veziyyet":"Yeni","guc_kw":7.5,"dovr":1450}'::jsonb, v_user, v_user)
    RETURNING id INTO i_motor;

    INSERT INTO ces_service.inventory_items
        (branch_id, node_id, category_id, name, sku, unit, quantity, purchase_price,
         is_serialized, attributes, created_by, updated_by)
    VALUES (v_branch, n_ref_b2, c_motor, 'Hidravlik nasos HP-40', 'HYD-40', 'ədəd', 2, 3200.00,
            TRUE, '{"istehsalci":"Bosch Rexroth","veziyyet":"Yeni"}'::jsonb, v_user, v_user)
    RETURNING id INTO i_hidravlik;

    -- Xırda bərkidicilər: sayla izlənir, Paket səviyyəsində.
    INSERT INTO ces_service.inventory_items
        (branch_id, node_id, category_id, name, sku, unit, quantity, purchase_price,
         is_serialized, attributes, created_by, updated_by)
    VALUES (v_branch, n_paket_a1_01_01, c_berkidici, 'Vint M8×40 DIN933', 'VNT-M8-40', 'ədəd', 480, 0.35,
            FALSE, '{"olcu":"M8","uzunluq":40,"istehsalci":"Würth"}'::jsonb, v_user, v_user)
    RETURNING id INTO i_vint;

    INSERT INTO ces_service.inventory_items
        (branch_id, node_id, category_id, name, sku, unit, quantity, purchase_price,
         is_serialized, attributes, created_by, updated_by)
    VALUES (v_branch, n_paket_a1_01_02, c_berkidici, 'Qayka M10 DIN934', 'QYK-M10', 'ədəd', 260, 0.22,
            FALSE, '{"olcu":"M10","istehsalci":"Würth"}'::jsonb, v_user, v_user)
    RETURNING id INTO i_qayka;

    -- Qutu səviyyəsində (paket açılmayıb).
    INSERT INTO ces_service.inventory_items
        (branch_id, node_id, category_id, name, sku, unit, quantity, purchase_price,
         is_serialized, attributes, created_by, updated_by)
    VALUES (v_branch, n_qutu_a1_02, c_elektrik, 'Klemma 4mm² boz', 'KLM-4', 'ədəd', 150, 0.90,
            FALSE, '{"gerginlik":400,"istehsalci":"Phoenix Contact"}'::jsonb, v_user, v_user)
    RETURNING id INTO i_klemma;

    INSERT INTO ces_service.inventory_items
        (branch_id, node_id, category_id, name, sku, unit, quantity, purchase_price,
         is_serialized, attributes, created_by, updated_by)
    VALUES (v_branch, n_ref_a2, c_elektrik, 'Güc kabeli 3×2.5 mm²', 'KBL-3X25', 'metr', 320, 2.10,
            FALSE, '{"gerginlik":400,"istehsalci":"Nexans"}'::jsonb, v_user, v_user)
    RETURNING id INTO i_kabel;

    INSERT INTO ces_service.inventory_items
        (branch_id, node_id, category_id, name, sku, unit, quantity, purchase_price,
         is_serialized, attributes, created_by, updated_by)
    VALUES (v_branch, n_qutu_c1_01, c_filtr, 'Yağ filtri W950/4', 'FLT-W950', 'ədəd', 24, 12.50,
            FALSE, '{"uygunluq":"CAT 320D","istehsalci":"Mann"}'::jsonb, v_user, v_user)
    RETURNING id INTO i_filtr_yag;

    INSERT INTO ces_service.inventory_items
        (branch_id, node_id, category_id, name, sku, unit, quantity, purchase_price,
         is_serialized, attributes, created_by, updated_by)
    VALUES (v_branch, n_ref_c1, c_yag, 'Hidravlik yağ HLP 46', 'YAG-HLP46', 'litr', 200, 3.80,
            FALSE, '{"viskozite":"ISO VG 46","istehsalci":"Shell"}'::jsonb, v_user, v_user);

    -- ── Seriyalı vahidlər ────────────────────────────────────────────────────
    -- Sənin qaldırdığın hal: 5 motor qutuya yığılmır, hərəsi ayrıca izlənir.
    -- node_id item ilə eyni Ref-dir — vahidlər də dərinlik tələb etmir.
    INSERT INTO ces_service.inventory_item_units
        (branch_id, item_id, node_id, serial_number, status, purchase_date,
         warranty_start_date, warranty_end_date, created_by, updated_by)
    SELECT v_branch, i_motor, n_ref_b1, 'WEG-7K5-' || LPAD(g::text, 4, '0'), 'IN_STOCK',
           CURRENT_DATE - 30, CURRENT_DATE - 30, CURRENT_DATE + 700, v_user, v_user
    FROM generate_series(1, 5) AS g;

    INSERT INTO ces_service.inventory_item_units
        (branch_id, item_id, node_id, serial_number, status, purchase_date,
         warranty_start_date, warranty_end_date, created_by, updated_by)
    SELECT v_branch, i_hidravlik, n_ref_b2, 'HP40-' || LPAD(g::text, 4, '0'), 'IN_STOCK',
           CURRENT_DATE - 120, CURRENT_DATE - 120, CURRENT_DATE + 245, v_user, v_user
    FROM generate_series(1, 2) AS g;
END $$;
