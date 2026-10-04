package com.microservices.common.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxRepository {
    private static final String CLAIM_NEXT = """
            SELECT o.sequence, o.event_id, o.aggregate_id, o.event_type, o.topic,
                   o.payload::text, o.attempt_count
            FROM outbox_events o
            WHERE o.published_at IS NULL AND o.available_at <= CURRENT_TIMESTAMP
              AND NOT EXISTS (
                SELECT 1 FROM outbox_events earlier
                WHERE earlier.aggregate_id = o.aggregate_id AND earlier.published_at IS NULL
                  AND earlier.sequence < o.sequence)
            ORDER BY o.sequence LIMIT 1 FOR UPDATE OF o SKIP LOCKED
            """;

    private final JdbcTemplate jdbc;

    public void insert(EventTopics type, String aggregateId, String topic, EventCodec.Encoded encoded) {
        lockAggregate(aggregateId);
        jdbc.update("""
                INSERT INTO outbox_events(event_id, aggregate_id, event_type, topic, payload)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb))
                """, encoded.eventId(), aggregateId, type.name(), topic, encoded.payload());
    }

    public Optional<PendingEvent> claimNext() {
        return jdbc.query(CLAIM_NEXT, (rs, row) -> new PendingEvent(rs.getObject(2, UUID.class),
                        rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7)))
                .stream().findFirst();
    }

    public void markPublished(UUID eventId) {
        jdbc.update("""
                UPDATE outbox_events SET published_at = CURRENT_TIMESTAMP, last_error = NULL WHERE event_id = ?
                """, eventId);
    }

    public void scheduleRetry(UUID eventId, int delaySeconds, String error) {
        jdbc.update("""
                UPDATE outbox_events SET attempt_count = attempt_count + 1,
                  available_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second'), last_error = ?
                WHERE event_id = ?
                """, delaySeconds, error, eventId);
    }

    private void lockAggregate(String aggregateId) {
        jdbc.execute(connection -> {
            var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))");
            statement.setString(1, aggregateId);
            return statement;
        }, (PreparedStatementCallback<Void>) statement -> {
            statement.execute();
            return null;
        });
    }

    public record PendingEvent(UUID eventId, String aggregateId, String eventType,
                               String topic, String payload, int attempts) {
    }
}
