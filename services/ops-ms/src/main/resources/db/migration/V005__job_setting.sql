-- public.job_setting definition

-- Drop table

-- DROP TABLE public.job_setting;

CREATE TABLE IF NOT EXISTS public.job_setting (
                                    id bigserial NOT NULL,
                                    job_name varchar(100) NOT NULL,
                                    initiator varchar(50) NOT NULL,
                                    description varchar(500) NULL,
                                    execution_scope varchar(100) NOT NULL,
                                    affected_records int8 DEFAULT 0 NOT NULL,
                                    started_at timestamptz NOT NULL,
                                    finished_at timestamptz NULL,
                                    execution_time_ms int8 NULL,
                                    status varchar(20) NOT NULL,
                                    created_at timestamptz DEFAULT now() NOT NULL,
                                    updated_at timestamptz DEFAULT now() NOT NULL,
                                    error_message text NULL,
                                    CONSTRAINT job_setting_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_job_setting_created_at ON public.job_setting USING btree (created_at);
CREATE INDEX IF NOT EXISTS idx_job_setting_job_name ON public.job_setting USING btree (job_name);