CREATE TABLE order_confirmation_outbox (
    event_id UUID PRIMARY KEY,
    order_id INTEGER NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX order_confirmation_outbox_pending_idx
    ON order_confirmation_outbox (created_at, event_id);
