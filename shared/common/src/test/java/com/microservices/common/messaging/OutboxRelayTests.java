package com.microservices.common.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class OutboxRelayTests {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final EventCodec codec = new EventCodec(mapper);
    private final UUID eventId = UUID.randomUUID();
    private final OrderConfirmationEvent event = new OrderConfirmationEvent(eventId, "7", "ORD-7",
            BigDecimal.TEN, PaymentMethod.VISA, "c1", List.of());
    private final OutboxRelay relay = new OutboxRelay(new OutboxRepository(jdbc), kafka, codec, manager,
            new MessagingProperties(java.util.Map.of(), 2, 1, 500, 1000));
    private ResultSet row;

    @BeforeEach
    void setUp() throws Exception {
        when(manager.getTransaction(any())).thenAnswer(ignored -> new SimpleTransactionStatus());
        row = mock(ResultSet.class);
        when(row.getLong(1)).thenReturn(1L);
        when(row.getObject(2, UUID.class)).thenReturn(eventId);
        when(row.getString(3)).thenReturn("7");
        when(row.getString(4)).thenReturn("ORDER_CONFIRMATION");
        when(row.getString(5)).thenReturn("configured-topic");
        when(row.getString(6)).thenReturn(mapper.writeValueAsString(event));
        when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<?> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(row, 0));
        });
        when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void acknowledgementPrecedesPublishedMarkerAndTransactionCommit() {
        assertTrue(relay.publishNext());
        ArgumentCaptor<ProducerRecord<String, Object>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        var sequence = inOrder(kafka, jdbc, manager);
        sequence.verify(kafka).send(captor.capture());
        sequence.verify(jdbc).update(startsWith("UPDATE outbox_events SET published_at"), eq(eventId));
        sequence.verify(manager).commit(any());
        var record = captor.getValue();
        assertEquals("configured-topic", record.topic());
        assertEquals("7", record.key());
        assertEquals(event, record.value());
        assertEquals(eventId.toString(), new String(record.headers().lastHeader("eventId").value(), StandardCharsets.UTF_8));
    }

    @Test
    void failedSendPersistsBackoffAndRetryKeepsIdentity() {
        when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException("offline")))
                .thenReturn(CompletableFuture.completedFuture(null));
        assertTrue(relay.publishNext());
        verify(jdbc).update(contains("attempt_count = attempt_count + 1"), eq(1), eq("ExecutionException"), eq(eventId));
        verify(jdbc, never()).update(startsWith("UPDATE outbox_events SET published_at"), any(UUID.class));
        assertTrue(relay.publishNext());
        verify(jdbc).update(startsWith("UPDATE outbox_events SET published_at"), eq(eventId));
        verify(manager, times(2)).commit(any());
    }

    @Test
    void retryDelayCapsAtFiveMinutes() throws Exception {
        when(row.getInt(7)).thenReturn(20);
        when(kafka.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
        relay.publishNext();
        verify(jdbc).update(contains("attempt_count = attempt_count + 1"), eq(300), anyString(), eq(eventId));
    }

    @Test
    void mismatchedStoredIdentityDoesNotPublish() throws Exception {
        when(row.getObject(2, UUID.class)).thenReturn(UUID.randomUUID());
        relay.publishNext();
        verifyNoInteractions(kafka);
        verify(jdbc).update(contains("attempt_count = attempt_count + 1"), eq(1), eq("IllegalArgumentException"), any(UUID.class));
    }

    @Test
    void timeoutLeavesPendingRecord() {
        when(kafka.send(any(ProducerRecord.class))).thenReturn(new CompletableFuture<>());
        relay.publishNext();
        verify(jdbc).update(contains("attempt_count = attempt_count + 1"), eq(1), eq("TimeoutException"), eq(eventId));
    }

    @Test
    void interruptionRestoresFlagAndStopsBatch() {
        when(kafka.send(any(ProducerRecord.class))).thenAnswer(ignored -> {
            Thread.currentThread().interrupt();
            return new CompletableFuture<>();
        });
        try {
            relay.drain();
            assertTrue(Thread.currentThread().isInterrupted());
            verify(kafka, times(1)).send(any(ProducerRecord.class));
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void databaseFailureAfterAcknowledgementRollsBack() {
        when(jdbc.update(startsWith("UPDATE outbox_events SET published_at"), eq(eventId)))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, relay::publishNext);
        verify(manager).rollback(any());
        verify(manager, never()).commit(any());
    }

    @Test
    void boundedBatchUsesSeparateNewTransactions() {
        relay.drain();
        verify(kafka, times(2)).send(any(ProducerRecord.class));
        verify(manager, times(2)).getTransaction(argThat(definition ->
                definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(manager, times(2)).commit(any());
    }

    @Test
    void emptyQueueStopsBatch() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
        relay.drain();
        verifyNoInteractions(kafka);
        verify(manager, times(1)).commit(any());
    }
}
