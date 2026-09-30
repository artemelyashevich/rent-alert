INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (1, 'KUFAR', 'Брест', 'country-belarus~province-brestskaja_oblast~locality-brest', '2026-09-01 06:33:22.029', '2026-09-01 06:33:22.029', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (2, 'KUFAR', 'Минск', 'country-belarus~province-minsk~locality-minsk', '2026-09-01 06:34:01.034', '2026-09-01 06:34:01.034', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (3, 'KUFAR', 'Витебск', 'country-belarus~province-vitebskaja_oblast~locality-vitebsk', '2026-09-01 06:34:25.907', '2026-09-01 06:34:25.907', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (4, 'KUFAR', 'Гомель', 'country-belarus~province-gomelskaja_oblast~locality-gomel', '2026-09-01 06:34:46.138', '2026-09-01 06:34:46.138', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (5, 'KUFAR', 'Гродно', 'country-belarus~province-grodnenskaja_oblast~locality-grodno', '2026-09-01 06:35:12.838', '2026-09-01 06:35:12.838', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (6, 'KUFAR', 'Могилёв', 'country-belarus~province-mogilyovskaja_oblast~locality-mogilyov', '2026-09-01 06:35:39.126', '2026-09-01 06:35:39.126', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (14, 'ONLINER', 'Брест',
        '&bounds%5Blb%5D%5Blat%5D=51.80776565156935&bounds%5Blb%5D%5Blong%5D=23.538208007812504&bounds%5Brt%5D%5Blat%5D=52.36511830615023&bounds%5Brt%5D%5Blong%5D=23.881530761718754',
        '2026-09-04 09:02:36.273', '2026-09-04 09:02:36.273', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (15, 'ONLINER', 'Минск',
        '&bounds%5Blb%5D%5Blat%5D=53.63405351645887&bounds%5Blb%5D%5Blong%5D=27.362823486328125&bounds%5Brt%5D%5Blat%5D=54.16846388288719&bounds%5Brt%5D%5Blong%5D=27.76107788085938',
        '2026-09-04 09:03:47.573', '2026-09-04 09:03:47.573', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (16, 'ONLINER', 'Гродно',
        '&bounds%5Blb%5D%5Blat%5D=53.40994115453626&bounds%5Blb%5D%5Blong%5D=23.67485046386719&bounds%5Brt%5D%5Blat%5D=53.94719601691825&bounds%5Brt%5D%5Blong%5D=24.01817321777344',
        '2026-09-04 09:10:32.364', '2026-09-04 09:10:32.364', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (17, 'ONLINER', 'Гомель',
        '&bounds%5Blb%5D%5Blat%5D=51.9010708309271&bounds%5Blb%5D%5Blong%5D=30.616149902343754&bounds%5Brt%5D%5Blat%5D=53.00652061338904&bounds%5Brt%5D%5Blong%5D=31.30279541015625',
        '2026-09-04 09:11:08.663', '2026-09-04 09:11:08.663', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (18, 'ONLINER', 'Витебск',
        '&bounds%5Blb%5D%5Blat%5D=54.963424881416785&bounds%5Blb%5D%5Blong%5D=30.02769470214844&bounds%5Brt%5D%5Blat%5D=55.480799060911195&bounds%5Brt%5D%5Blong%5D=30.37101745605469',
        '2026-09-04 09:11:45.162', '2026-09-04 09:11:45.162', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (20, 'ONLINER', 'Могилёв',
        '&bounds%5Blb%5D%5Blat%5D=53.61369161186367&bounds%5Blb%5D%5Blong%5D=30.15060424804688&bounds%5Brt%5D%5Blat%5D=54.148360749133566&bounds%5Brt%5D%5Blong%5D=30.54885864257813',
        '2026-09-04 18:39:58.136', '2026-09-04 18:39:58.136', true) ON CONFLICT DO NOTHING;
INSERT INTO public.cities
    (id, provider, "name", city_code, created_at, updated_at, is_active)
VALUES (13, 'REALT', 'Минск', '4c8f8db2-7b00-11eb-8943-0cc47adabd66', '2026-09-01 06:49:23.399', '2026-09-08 08:41:54.459', false) ON CONFLICT DO NOTHING;

INSERT INTO public.scheduler_settings
(id, provider_name, created_at, updated_at, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to, price_currency, region_id, sort_by, sort_order, category_id,
 room_category_id)
VALUES (3, 'ONLINER', '2026-08-21 23:50:19.665', '2026-09-08 11:41:16.622', true, true, 60000, 100, 0, 1000, 'USD', '53.629981921409794,27.364883422851566,54.16444403731646,27.760391235351562',
        'created_at', 'DESC', 2, NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.scheduler_settings
(id, provider_name, created_at, updated_at, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to, price_currency, region_id, sort_by, sort_order, category_id,
 room_category_id)
VALUES (1, 'KUFAR', '2026-08-21 23:50:15.808', '2026-09-08 11:41:24.518', true, true, 45000, 100, 0, 1000, 'USD', 'country-belarus~province-minsk~locality-minsk', 'lst.d', 'DESC', 1010,
        1040) ON CONFLICT DO NOTHING;
INSERT INTO public.scheduler_settings
(id, provider_name, created_at, updated_at, scheduler_enabled, notifications_enabled, fixed_rate_ms, page_size, price_from, price_to, price_currency, region_id, sort_by, sort_order, category_id,
 room_category_id)
VALUES (2, 'REALT', '2026-08-21 23:50:16.069', '2026-08-29 17:52:57.327', true, true, 60000, 100, 0, 1000, 'USD', '4cb07174-7b00-11eb-8943-0cc47adabd66', 'newAgainDate', 'DESC', 2,
        4) ON CONFLICT DO NOTHING;

INSERT INTO public.schedule_tasks
    (id, last_processed_id, attempt, provider, scrap, last_sync_at, created_at, updated_at)
VALUES (1, NULL, 24076, 'KUFAR', false, '2026-09-30 13:34:41.466', '2026-08-21 23:50:38.013', '2026-09-30 13:34:41.532') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_tasks
    (id, last_processed_id, attempt, provider, scrap, last_sync_at, created_at, updated_at)
VALUES (3, NULL, 10502, 'REALT', false, '2026-09-30 13:34:41.640', '2026-08-21 23:50:39.334', '2026-09-30 13:34:41.686') ON CONFLICT DO NOTHING;
INSERT INTO public.schedule_tasks
    (id, last_processed_id, attempt, provider, scrap, last_sync_at, created_at, updated_at)
VALUES (2, NULL, 11884, 'ONLINER', false, '2026-09-30 13:34:41.797', '2026-08-21 23:50:39.156', '2026-09-30 13:34:41.844') ON CONFLICT DO NOTHING;

-- Rows above carry explicit ids: move the bigserial sequences past them
SELECT setval(pg_get_serial_sequence('public.cities', 'id'), (SELECT max(id) FROM public.cities));
SELECT setval(pg_get_serial_sequence('public.scheduler_settings', 'id'), (SELECT max(id) FROM public.scheduler_settings));
SELECT setval(pg_get_serial_sequence('public.schedule_tasks', 'id'), (SELECT max(id) FROM public.schedule_tasks));
