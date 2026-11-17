CREATE TABLE IF NOT EXISTS outbox_event
(
    id           BIGSERIAL                NOT NULL,
    topic        VARCHAR(255)             NOT NULL,
    message_key  VARCHAR(255),
    payload      TEXT                     NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at TIMESTAMP WITH TIME ZONE,
    attempts     INTEGER                  NOT NULL DEFAULT 0,
    last_error   TEXT,
    trace_id     VARCHAR(64),

    CONSTRAINT outbox_event_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_outbox_event_unpublished ON outbox_event (id) WHERE published_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_outbox_event_published_at ON outbox_event (published_at) WHERE published_at IS NOT NULL;
