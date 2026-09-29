-- Transactional outbox (kafka-util OutboxPublisher / OutboxRelay). Copy into the service's Flyway migrations.

CREATE TABLE IF NOT EXISTS outbox_event (
    id           bigserial    NOT NULL,
    topic        varchar(255) NOT NULL,
    message_key  varchar(255) NULL,
    payload      text         NOT NULL,
    created_at   timestamptz  DEFAULT now() NOT NULL,
    published_at timestamptz  NULL,
    attempts     int4         DEFAULT 0 NOT NULL,
    last_error   text         NULL,
    CONSTRAINT outbox_event_pkey PRIMARY KEY (id)
);
-- The relay only reads unpublished rows
CREATE INDEX IF NOT EXISTS idx_outbox_event_unpublished ON outbox_event (id) WHERE published_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_outbox_event_published_at ON outbox_event (published_at) WHERE published_at IS NOT NULL;
