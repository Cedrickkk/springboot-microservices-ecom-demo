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

INSERT INTO outbox_events (event_id, aggregate_id, event_type, topic, payload, created_at)
SELECT event_id,
       order_id::text,
       'ORDER_CONFIRMATION',
       '${orderConfirmationTopic}',
       payload::jsonb || jsonb_build_object('eventId', event_id::text, 'aggregateId', order_id::text),
       created_at
FROM order_confirmation_outbox
ORDER BY created_at, event_id;

DROP TABLE order_confirmation_outbox;
