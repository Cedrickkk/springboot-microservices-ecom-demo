package com.microservices.common.messaging;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record PaymentConfirmationEvent(
        UUID eventId,
        String aggregateId,
        Integer orderId,
        String orderReference,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        String customerId,
        String customerFirstname,
        String customerLastname,
        String customerEmail) implements MessagingEvent {

    public PaymentConfirmationEvent {
        Objects.requireNonNull(eventId, "eventId");
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId must not be blank");
        }
    }
}
