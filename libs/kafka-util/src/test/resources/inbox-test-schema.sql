CREATE TABLE inbox_event
(
    id              bigserial                              NOT NULL PRIMARY KEY,
    consumer        varchar(128)                           NOT NULL,
    idempotency_key uuid                                   NOT NULL,
    processed_at    timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT uq_inbox_event_consumer_key UNIQUE (consumer, idempotency_key)
);
CREATE INDEX idx_inbox_event_processed_at ON inbox_event (processed_at);
