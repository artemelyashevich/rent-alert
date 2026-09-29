-- How each NotificationType is delivered; edited here instead of in the code (cache refresh: app.notification.type-settings-refresh)
-- engines: comma-separated NotificationEngine names; mandatory: sent even on channels the user switched off;
-- enabled: false stops delivery of the type without a release

CREATE TABLE IF NOT EXISTS public.notification_type_settings (
    id                bigserial    NOT NULL,
    notification_type varchar(50)  NOT NULL,
    category          varchar(50)  NOT NULL,
    engines           varchar(255) NOT NULL,
    mandatory         bool         DEFAULT false NOT NULL,
    enabled           bool         DEFAULT true NOT NULL,
    created_at        timestamptz  DEFAULT now() NOT NULL,
    updated_at        timestamptz  DEFAULT now() NOT NULL,
    CONSTRAINT notification_type_settings_pkey PRIMARY KEY (id),
    CONSTRAINT uk_notification_type_settings_type UNIQUE (notification_type)
);

INSERT INTO public.notification_type_settings (notification_type, category, engines) VALUES
    ('APP_STARTED',                'OPS_ALERTS',       'TELEGRAM_ADMIN'),
    ('APP_STOPPED',                'OPS_ALERTS',       'TELEGRAM_ADMIN'),
    ('NEW_LISTING_ADMIN_ALERT',    'SCRAPER_ALERTS',   'TELEGRAM_ADMIN'),
    ('USER_CREATED',               'OPS_ALERTS',       'TELEGRAM_ADMIN'),
    ('NEW_LISTING_MATCHED',        'LISTING_ALERTS',   'TELEGRAM_BOT'),
    ('FAVORITE_REMINDER',          'REMINDER_ALERTS',  'TELEGRAM_BOT'),
    ('EMAIL_ACTIVATION',           'EMAIL_ALERTS',     'EMAIL'),
    ('EMAIL_ACTIVATION_SUCCESS',   'EMAIL_ALERTS',     'EMAIL'),
    ('NEW_FEEDBACK',               'OPS_ALERTS',       'TELEGRAM_ADMIN'),
    ('BOT_BLOCKED',                'OPS_ALERTS',       'TELEGRAM_ADMIN'),
    ('BOT_UNBLOCKED',              'OPS_ALERTS',       'TELEGRAM_ADMIN'),
    ('ADMIN_BROADCAST',            'BROADCAST_ALERTS', 'TELEGRAM_BOT'),
    ('ROOMMATE_NEW_INTEREST',      'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_NEW_CANDIDATES',    'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_MATCHED',           'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_CHAT_STARTED',      'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_VIEWING_SCHEDULED', 'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_VIEWING_REMINDER',  'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_MATCH_ENDED',       'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_INTEREST_EXPIRED',  'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_PROFILE_APPROVED',  'ROOMMATE_ALERTS',  'TELEGRAM_BOT'),
    ('ROOMMATE_PROFILE_REJECTED',  'ROOMMATE_ALERTS',  'TELEGRAM_BOT')
ON CONFLICT (notification_type) DO NOTHING;
