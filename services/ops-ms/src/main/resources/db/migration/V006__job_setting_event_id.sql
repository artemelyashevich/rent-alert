-- job_setting.event_id makes a redelivered Kafka job event a no-op (JobSetting.eventId)
ALTER TABLE public.job_setting
    ADD COLUMN IF NOT EXISTS event_id uuid;
CREATE UNIQUE INDEX IF NOT EXISTS uk_job_setting_event_id ON public.job_setting USING btree (event_id);
