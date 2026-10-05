package com.microservices.ecom.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class KafkaConfigurationTests {
    private KafkaTemplate<String, String> template;
    private DeadLetterPublishingRecoverer recoverer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        template = mock(KafkaTemplate.class);
        when(template.partitionsFor("custom-payment.DLT")).thenReturn(List.of(
                new PartitionInfo("custom-payment.DLT", 2, null, new Node[0], new Node[0])));
        recoverer = new KafkaConfiguration().deadLetterPublishingRecoverer(template);
    }

    @Test
    void routesRawPayloadToDeclaredDeadLetterTopicWithOriginalKeyAndPartition() {
        when(template.send(any(ProducerRecord.class))).thenAnswer(invocation ->
                CompletableFuture.completedFuture(new SendResult<>(invocation.getArgument(0), null)));
        recoverer.accept(new ConsumerRecord<>("custom-payment", 2, 5L, "7", "{broken"),
                new IllegalArgumentException("Malformed payload"));
        verify(template).send(argThat((ProducerRecord<String, String> record) ->
                record.topic().equals("custom-payment.DLT") && record.partition() == 2
                        && record.key().equals("7") && record.value().equals("{broken")));
    }

    @Test
    void failedDeadLetterSendIsNotTreatedAsRecovery() {
        when(template.send(any(ProducerRecord.class))).thenReturn(
                CompletableFuture.failedFuture(new IllegalStateException("Broker unavailable")));
        assertThrows(KafkaException.class, () -> recoverer.accept(
                new ConsumerRecord<>("custom-payment", 2, 5L, "7", "payload"),
                new IllegalStateException("Processing failed")));
    }
}
