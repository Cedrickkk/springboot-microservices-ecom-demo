package com.microservices.common.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Map;

@ConfigurationProperties("application.messaging")
public record MessagingProperties(
        Map<EventTopics, String> topics,
        @DefaultValue("50") int outboxBatchSize,
        @DefaultValue("10000") long publishTimeoutMs,
        @DefaultValue("500") long outboxPollIntervalMs,
        @DefaultValue("1000") long outboxInitialDelayMs) {

    public MessagingProperties {
        topics = topics == null ? Map.of() : Map.copyOf(topics);
        topics.forEach((type, topic) -> {
            if (topic.isBlank()) {
                throw new IllegalArgumentException("Kafka topic for " + type + " must not be blank");
            }
        });
        if (outboxBatchSize <= 0 || publishTimeoutMs <= 0 || outboxPollIntervalMs <= 0 || outboxInitialDelayMs < 0) {
            throw new IllegalArgumentException("Batch size, publish timeout and poll interval must be positive; initial delay must not be negative");
        }
    }
}
