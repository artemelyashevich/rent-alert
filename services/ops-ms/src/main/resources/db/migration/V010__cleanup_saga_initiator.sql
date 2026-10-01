ALTER TABLE public.cleanup_saga
    ADD COLUMN IF NOT EXISTS initiator varchar(20) DEFAULT 'INTERNAL' NOT NULL;
