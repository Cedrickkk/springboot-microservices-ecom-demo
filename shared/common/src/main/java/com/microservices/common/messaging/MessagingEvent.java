package com.microservices.common.messaging;

import java.util.UUID;

public interface MessagingEvent {
    UUID eventId();
    String aggregateId();
}
