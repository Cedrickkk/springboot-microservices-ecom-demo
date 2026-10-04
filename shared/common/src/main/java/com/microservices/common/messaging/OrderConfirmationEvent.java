package com.microservices.common.messaging;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OrderConfirmationEvent(
        UUID eventId,
        String aggregateId,
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        String customerId, List<Product> products) implements MessagingEvent {

    public OrderConfirmationEvent {
        Objects.requireNonNull(eventId, "eventId");
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId must not be blank");
        }
        products = List.copyOf(products);
    }

    public record Product(
            Integer productId,
            String name,
            String description,
            BigDecimal price,
            double quantity
    ) {
    }
}
