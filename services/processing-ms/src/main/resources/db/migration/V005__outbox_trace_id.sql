ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS trace_id varchar (64) NULL;
