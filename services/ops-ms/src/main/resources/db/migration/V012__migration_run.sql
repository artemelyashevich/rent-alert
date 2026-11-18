CREATE TABLE IF NOT EXISTS public.migration_run (
    id            uuid         NOT NULL,
    status        varchar(30)  NOT NULL,
    settings      jsonb        NOT NULL,
    requested_by  int8         NULL,
    created_at    timestamptz  NOT NULL,
    started_at    timestamptz  NULL,
    finished_at   timestamptz  NULL,
    CONSTRAINT migration_run_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_migration_run_status ON public.migration_run USING btree (status);

CREATE TABLE IF NOT EXISTS public.migration_run_step (
    migration_id  uuid         NOT NULL,
    step_order    int4         NOT NULL,
    service       varchar(100) NOT NULL,
    status        varchar(30)  NOT NULL,
    tables        jsonb        NULL,
    processed     int8         DEFAULT 0 NOT NULL,
    total         int8         DEFAULT 0 NOT NULL,
    error         text         NULL,
    attempts      int4         DEFAULT 0 NOT NULL,
    sent_at       timestamptz  NULL,
    started_at    timestamptz  NULL,
    finished_at   timestamptz  NULL,
    updated_at    timestamptz  NOT NULL,
    CONSTRAINT migration_run_step_pkey PRIMARY KEY (migration_id, step_order),
    CONSTRAINT fk_migration_run_step_run FOREIGN KEY (migration_id) REFERENCES public.migration_run (id)
);
CREATE INDEX IF NOT EXISTS idx_migration_run_step_status ON public.migration_run_step USING btree (status, updated_at);
