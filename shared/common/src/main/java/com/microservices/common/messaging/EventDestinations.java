package com.microservices.common.messaging;

import lombok.RequiredArgsConstructor;
import java.util.Objects;

@RequiredArgsConstructor
public final class EventDestinations {
    private final MessagingProperties properties;

    public String topic(EventTopics type) {
        Objects.requireNonNull(type, "Event type must not be null");
        return properties.topics().getOrDefault(type, type.defaultTopic());
    }
}
