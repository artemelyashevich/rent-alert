-- audit_log: one row per admin action; event_id makes a redelivered Kafka event a no-op

CREATE TABLE IF NOT EXISTS public.audit_log
(
    id
    bigserial
    NOT
    NULL,
    event_id
    uuid
    NULL,
    event_name
    varchar
(
    255
) NOT NULL,
    event_description text NOT NULL,
    user_id int8 NULL,
    event_initiator varchar
(
    50
) NOT NULL,
    event_status varchar
(
    50
) NOT NULL,
    created_at timestamptz DEFAULT now
(
) NOT NULL,
    updated_at timestamptz DEFAULT now
(
) NOT NULL,
    CONSTRAINT audit_log_pkey PRIMARY KEY
(
    id
)
    );

-- the table may come from the monolith schema without event_id
ALTER TABLE public.audit_log
    ADD COLUMN IF NOT EXISTS event_id uuid;
CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_log_event_id ON public.audit_log USING btree (event_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON public.audit_log USING btree (created_at);
