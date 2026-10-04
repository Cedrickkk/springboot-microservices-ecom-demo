CREATE SEQUENCE payments_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE payments (
    id INTEGER PRIMARY KEY,
    amount NUMERIC(38, 2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(255) NOT NULL,
    order_id INTEGER NOT NULL CHECK (order_id > 0),
    created_at TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP NOT NULL
);

CREATE INDEX payments_order_id_idx ON payments (order_id);

CREATE TABLE outbox_events
(
    sequence      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id      UUID        NOT NULL UNIQUE,
    aggregate_id  TEXT        NOT NULL,
    event_type    TEXT        NOT NULL,
    topic         TEXT        NOT NULL,
    payload       JSONB       NOT NULL,
    attempt_count INTEGER     NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    available_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at  TIMESTAMPTZ,
    last_error    TEXT
);

CREATE INDEX outbox_events_pending_idx ON outbox_events (sequence) WHERE published_at IS NULL;
CREATE INDEX outbox_events_aggregate_pending_idx ON outbox_events (aggregate_id, sequence) WHERE published_at IS NULL;
