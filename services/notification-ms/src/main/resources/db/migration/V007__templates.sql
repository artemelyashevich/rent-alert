INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (1, 'NEW_LISTING_MATCHED', 'TELEGRAM_BOT', '{{message}}', '2026-08-21 23:50:23.160', '2026-08-21 23:50:23.160', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (2, 'APP_STARTED', 'TELEGRAM_ADMIN', '{{message}}', '2026-08-21 23:50:23.160', '2026-08-21 23:50:23.160', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (3, 'NEW_LISTING_ADMIN_ALERT', 'TELEGRAM_ADMIN', '{{message}}', '2026-08-21 23:50:23.160', '2026-08-21 23:50:23.160', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (4, 'APP_STOPPED', 'TELEGRAM_ADMIN', '{{message}}', '2026-08-21 23:50:23.713', '2026-08-21 23:50:23.713', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (6, 'EMAIL_ACTIVATION', 'EMAIL', '{{otp}}', '2026-08-21 23:50:27.810', '2026-08-21 23:50:27.810', 'email_activation') ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (7, 'EMAIL_ACTIVATION_SUCCESS', 'EMAIL', 'Ваша почта успешно активирована.', '2026-08-21 23:50:28.078', '2026-08-21 23:50:28.078', 'email_activation_success') ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (8, 'ADMIN_BROADCAST', 'TELEGRAM_BOT', '{{message}}', '2026-08-23 18:30:16.255', '2026-08-23 18:30:16.255', 'admin_broadcast') ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (9, 'USER_CREATED', 'TELEGRAM_ADMIN', 'Новый пользователь: "{{message}}" успешно зарегистрировался', '2026-08-23 21:16:40.201', '2026-08-23 21:16:40.201', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (10, 'NEW_FEEDBACK', 'TELEGRAM_ADMIN', 'Пользователь с id: "{{message}}" оставил новый отзыв!', '2026-08-26 16:59:37.352', '2026-08-26 16:59:37.352', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (11, 'BOT_BLOCKED', 'TELEGRAM_ADMIN', 'Пользователь с id: "{{message}}" заблокировал бота!', '2026-08-26 16:59:37.352', '2026-08-26 16:59:37.352', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (12, 'BOT_UNBLOCKED', 'TELEGRAM_ADMIN', 'Пользователь с id: "{{message}}" разблокировал бота!', '2026-08-26 16:59:37.352', '2026-08-26 16:59:37.352', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (5, 'FAVORITE_REMINDER', 'TELEGRAM_BOT', '🔔 <b>Скоро просмотр квартиры</b>

{{message}}

Не пропустите!', '2026-08-21 23:50:25.202', '2026-08-29 17:53:42.332', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (13, 'ROOMMATE_NEW_INTEREST', 'TELEGRAM_BOT', '👀 {{message}}', '2026-09-04 17:42:12.122', '2026-09-04 17:42:12.122', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (14, 'ROOMMATE_NEW_CANDIDATES', 'TELEGRAM_BOT', '👥 {{message}}', '2026-09-04 17:42:12.122', '2026-09-04 17:42:12.122', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (15, 'ROOMMATE_MATCHED', 'TELEGRAM_BOT', '🎉 {{message}}', '2026-09-04 17:42:12.122', '2026-09-04 17:42:12.122', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (16, 'ROOMMATE_CHAT_STARTED', 'TELEGRAM_BOT', '💬 {{message}}', '2026-09-04 17:42:12.122', '2026-09-04 17:42:12.122', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (17, 'ROOMMATE_VIEWING_SCHEDULED', 'TELEGRAM_BOT', '🗓 {{message}}', '2026-09-04 17:42:12.122', '2026-09-04 17:42:12.122', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (18, 'ROOMMATE_VIEWING_REMINDER', 'TELEGRAM_BOT', '🔔 {{message}}', '2026-09-04 17:42:12.122', '2026-09-04 17:42:12.122', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (19, 'ROOMMATE_MATCH_ENDED', 'TELEGRAM_BOT', '{{message}}', '2026-09-04 17:42:12.122', '2026-09-04 17:42:12.122', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (20, 'ROOMMATE_INTEREST_EXPIRED', 'TELEGRAM_BOT', '{{message}}', '2026-09-04 17:42:12.122', '2026-09-04 17:42:12.122', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (21, 'ROOMMATE_PROFILE_APPROVED', 'TELEGRAM_BOT', '✅ {{message}}', '2026-09-04 17:42:12.650', '2026-09-04 17:42:12.650', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (22, 'ROOMMATE_PROFILE_REJECTED', 'TELEGRAM_BOT', '⚠️ {{message}}', '2026-09-04 17:42:12.650', '2026-09-04 17:42:12.650', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (23, 'NEW_LISTING_MATCHED', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (24, 'ROOMMATE_NEW_INTEREST', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (25, 'ROOMMATE_NEW_CANDIDATES', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (26, 'ROOMMATE_MATCHED', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (27, 'ROOMMATE_CHAT_STARTED', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (28, 'ROOMMATE_VIEWING_SCHEDULED', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (29, 'ROOMMATE_MATCH_ENDED', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (30, 'ROOMMATE_INTEREST_EXPIRED', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (31, 'ROOMMATE_PROFILE_APPROVED', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;
INSERT INTO public.notification_templates
    (id, notification_type, engine, template_text, created_at, updated_at, template_name)
VALUES (32, 'ROOMMATE_PROFILE_REJECTED', 'WEBSOCKET', '{{message}}', '2026-09-06 19:28:18.943', '2026-09-06 19:28:18.943', NULL) ON CONFLICT DO NOTHING;

-- Rows above carry explicit ids: move the bigserial sequence past them
SELECT setval(pg_get_serial_sequence('public.notification_templates', 'id'), (SELECT max(id) FROM public.notification_templates));
