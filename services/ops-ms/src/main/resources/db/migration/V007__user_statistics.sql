CREATE TABLE IF NOT EXISTS public.user_statistics (
    id          bigserial    NOT NULL,
    user_id     int8         NOT NULL,
    filter_id   int8         NULL,
    event_type  varchar(50)  NOT NULL,
    event_date  date         NOT NULL,
    count       int4         DEFAULT 1 NOT NULL,
    created_at  timestamptz  DEFAULT now() NOT NULL,
    updated_at  timestamptz  DEFAULT now() NOT NULL,
    CONSTRAINT user_statistics_pkey PRIMARY KEY (id),
    CONSTRAINT uk_user_statistics_user_filter_type_date
        UNIQUE NULLS NOT DISTINCT (user_id, filter_id, event_type, event_date)
);
CREATE INDEX IF NOT EXISTS idx_user_statistics_user_date ON public.user_statistics USING btree (user_id, event_date);
CREATE INDEX IF NOT EXISTS idx_user_statistics_user_filter_date ON public.user_statistics USING btree (user_id, filter_id, event_date);
