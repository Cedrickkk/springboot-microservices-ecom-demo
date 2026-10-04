package com.microservices.common.messaging;

import com.microservices.common.messaging.OutboxRepository.PendingEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class OutboxRelay {
    private static final int MAX_RETRY_DELAY_SECONDS = 300;

    private final OutboxRepository repository;
    private final KafkaTemplate<String, Object> kafka;
    private final EventCodec codec;
    private final TransactionTemplate transactions;
    private final MessagingProperties properties;

    public OutboxRelay(OutboxRepository repository, KafkaTemplate<String, Object> kafka, EventCodec codec,
                       PlatformTransactionManager transactionManager, MessagingProperties properties) {
        this.repository = repository;
        this.kafka = kafka;
        this.codec = codec;
        this.properties = properties;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelayString = "${application.messaging.outbox-poll-interval-ms:500}",
            initialDelayString = "${application.messaging.outbox-initial-delay-ms:1000}")
    public void drain() {
        try {
            for (int processed = 0; processed < properties.outboxBatchSize(); processed++) {
                if (Thread.currentThread().isInterrupted() || !publishNext()) {
                    break;
                }
            }
        } catch (RuntimeException e) {
            log.warn("Outbox batch failed; pending events will be retried", e);
        }
    }

    public boolean publishNext() {
        return Boolean.TRUE.equals(transactions.execute(ignored -> {
            var pending = repository.claimNext();
            if (pending.isEmpty()) {
                return false;
            }
            publishClaimed(pending.get());
            return true;
        }));
    }

    private void publishClaimed(PendingEvent event) {
        try {
            var type = EventTopics.valueOf(event.eventType());
            var message = codec.decode(type, event.payload());
            codec.validate(type, event.aggregateId(), message);
            if (!event.eventId().equals(message.eventId())) {
                throw new IllegalArgumentException("Stored outbox identity does not match event");
            }
            send(event, message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            retry(event, e);
            return;
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            retry(event, e);
            return;
        }
        repository.markPublished(event.eventId());
    }

    private void send(PendingEvent event, MessagingEvent message)
            throws InterruptedException, ExecutionException, TimeoutException {
        var record = new ProducerRecord<String, Object>(event.topic(), event.aggregateId(), message);
        record.headers().add("eventId", event.eventId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventType", event.eventType().getBytes(StandardCharsets.UTF_8));
        kafka.send(record).get(properties.publishTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    private void retry(PendingEvent event, Exception failure) {
        int delaySeconds = (int) Math.min(MAX_RETRY_DELAY_SECONDS, 1L << Math.min(event.attempts(), 30));
        repository.scheduleRetry(event.eventId(), delaySeconds, failure.getClass().getSimpleName());
        log.warn("Outbox event {} will retry in {} seconds after {}", event.eventId(), delaySeconds,
                failure.getClass().getSimpleName());
    }
}
