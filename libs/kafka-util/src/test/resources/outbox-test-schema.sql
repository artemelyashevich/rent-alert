CREATE TABLE outbox_event (
    id           bigserial    NOT NULL PRIMARY KEY,
    topic        varchar(255) NOT NULL,
    message_key  varchar(255) NULL,
    payload      text         NOT NULL,
    created_at   timestamp with time zone DEFAULT now() NOT NULL,
    published_at timestamp with time zone NULL,
    attempts     int          DEFAULT 0 NOT NULL,
    last_error   text         NULL
);
