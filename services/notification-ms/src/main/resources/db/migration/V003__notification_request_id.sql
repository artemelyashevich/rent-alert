-- request_id = NotificationRequest.requestId: a request redelivered by Kafka is queued only once per channel

ALTER TABLE public.notifications
    ADD COLUMN IF NOT EXISTS request_id uuid;
CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_request_engine
    ON public.notifications USING btree (request_id, engine) WHERE (request_id IS NOT NULL);
