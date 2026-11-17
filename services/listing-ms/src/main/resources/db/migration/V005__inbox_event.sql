CREATE TABLE IF NOT EXISTS inbox_event
(
    id              BIGSERIAL                NOT NULL,
    consumer        VARCHAR(128)             NOT NULL,
    idempotency_key UUID                     NOT NULL,
    processed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT inbox_event_pkey PRIMARY KEY (id),
    CONSTRAINT uq_inbox_event_consumer_key UNIQUE (consumer, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_inbox_event_processed_at ON inbox_event (processed_at);
