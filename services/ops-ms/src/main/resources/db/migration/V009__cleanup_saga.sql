CREATE TABLE IF NOT EXISTS public.cleanup_saga (
    id              bigserial    NOT NULL,
    saga_id         uuid         NOT NULL,
    status          varchar(30)  NOT NULL,
    reference_time  timestamptz  NOT NULL,
    finished_at     timestamptz  NULL,
    deleted         int8         DEFAULT 0 NOT NULL,
    failed          int8         DEFAULT 0 NOT NULL,
    created_at      timestamptz  DEFAULT now() NOT NULL,
    updated_at      timestamptz  DEFAULT now() NOT NULL,
    CONSTRAINT cleanup_saga_pkey PRIMARY KEY (id),
    CONSTRAINT uk_cleanup_saga_saga_id UNIQUE (saga_id)
);
CREATE INDEX IF NOT EXISTS idx_cleanup_saga_status ON public.cleanup_saga USING btree (status);

CREATE TABLE IF NOT EXISTS public.cleanup_saga_step (
    id           bigserial    NOT NULL,
    saga_id      uuid         NOT NULL,
    step_order   int4         NOT NULL,
    service      varchar(100) NOT NULL,
    status       varchar(30)  NOT NULL,
    attempts     int4         DEFAULT 0 NOT NULL,
    sent_at      timestamptz  NULL,
    finished_at  timestamptz  NULL,
    deleted      int8         DEFAULT 0 NOT NULL,
    failed       int8         DEFAULT 0 NOT NULL,
    duration_ms  int8         DEFAULT 0 NOT NULL,
    errors       text         NULL,
    created_at   timestamptz  DEFAULT now() NOT NULL,
    updated_at   timestamptz  DEFAULT now() NOT NULL,
    CONSTRAINT cleanup_saga_step_pkey PRIMARY KEY (id),
    CONSTRAINT uk_cleanup_saga_step_saga_order UNIQUE (saga_id, step_order)
);
CREATE INDEX IF NOT EXISTS idx_cleanup_saga_step_status ON public.cleanup_saga_step USING btree (status);
