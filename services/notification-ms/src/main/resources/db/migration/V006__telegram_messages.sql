-- telegram_messages: incoming bot messages (TelegramMessage)
-- user_id is a user of identity-ms (another database), so it isn't referenced

CREATE TABLE IF NOT EXISTS public.telegram_messages (
    id          bigserial    NOT NULL,
    user_id     int8         NULL,
    "text"      text         NULL,
    command     varchar(50)  NULL,
    created_at  timestamptz  DEFAULT now() NOT NULL,
    updated_at  timestamptz  DEFAULT now() NOT NULL,
    CONSTRAINT telegram_messages_pkey PRIMARY KEY (id)
);
