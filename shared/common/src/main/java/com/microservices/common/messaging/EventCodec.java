package com.microservices.common.messaging;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public final class EventCodec {
    private static final Map<EventTopics, Class<? extends MessagingEvent>> TYPES = Map.of(
            EventTopics.ORDER_CONFIRMATION, OrderConfirmationEvent.class,
            EventTopics.PAYMENT_CONFIRMATION, PaymentConfirmationEvent.class);

    private final JsonMapper mapper;

    public Encoded encode(EventTopics type, String aggregateId, MessagingEvent event) {
        validate(type, aggregateId, event);
        try {
            return new Encoded(event.eventId(), mapper.writeValueAsString(event));
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Cannot serialize event of type " + type, e);
        }
    }

    public MessagingEvent decode(EventTopics type, String payload) {
        var eventClass = eventClass(type);
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Stored event payload must not be blank");
        }
        final MessagingEvent event;
        try {
            event = mapper.readValue(payload, eventClass);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Invalid stored event of type " + type, e);
        }
        validate(type, event == null ? null : event.aggregateId(), event);
        return event;
    }

    public void validate(EventTopics type, String aggregateId, MessagingEvent event) {
        if (event == null || eventClass(type) != event.getClass()) {
            throw new IllegalArgumentException("Unsupported event/type pairing: " + type);
        }
        if (aggregateId == null || aggregateId.isBlank() || !aggregateId.equals(event.aggregateId())) {
            throw new IllegalArgumentException("Event aggregate does not match message key");
        }
        if (event.eventId() == null) {
            throw new IllegalArgumentException("Event ID must not be null");
        }
    }

    private static Class<? extends MessagingEvent> eventClass(EventTopics type) {
        var eventClass = type == null ? null : TYPES.get(type);
        if (eventClass == null) {
            throw new IllegalArgumentException("Unsupported event type: " + type);
        }
        return eventClass;
    }

    public record Encoded(UUID eventId, String payload) {
    }
}
